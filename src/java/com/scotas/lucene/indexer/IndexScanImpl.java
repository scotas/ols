package com.scotas.lucene.indexer;

/**
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import java.math.BigDecimal;

import java.rmi.RemoteException;

import java.sql.SQLException;

import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import com.scotas.lucene.search.CountHitCollector;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollector;
import com.scotas.lucene.store.OJVMDirectory;

import org.apache.lucene.search.IndexSearcher;


/**
 * Implements the Start/Fetch/Close of ODCI API
 */
public class IndexScanImpl implements IndexScan {

    /**
     * Java Util Logging variables and default values
     */
    private Logger logger = null;

    /**
     * Constant used to get Logger name
     */
    static final String CLASS_NAME = IndexScanImpl.class.getName();

    /**
     * @throws RemoteException
     */
    public IndexScanImpl() {
        super();
        logger = Logger.getLogger(CLASS_NAME);
        logger.setLevel(Level.WARNING);
    }

    /**
     * Prepare the ScanContext information to start quering Lucene index
     * also store hits to be consumed by fetchs operations
     * @param dir string with SCHEMA.IDX_NAME syntax
     * @param cmpval Lucene Query Syntax string lcontains second arg.
     * @param sort column string, lcontains third arg.
     * @param storeScore true if TextScore will required score values
     * @param firstRowHint true if Oracle FIRST_ROWS optimizer hint is present
     * @return integer value which defines the context created
     * @throws RemoteException
     */
    public int start(String dir, String cmpval, String sort,
                     boolean storeScore,
                     boolean firstRowHint) throws RemoteException {
        //logger.entering(CLASS_NAME, "start",
        //                new Object[] { dir, cmpval, sort, storeScore });
        ScanContext scanCtx = new ScanContext();
        TopDocs hits = null;
        int startIndex = 0;
        int endIndex = 0;
        String qryStr = null;
        IndexSearcher searcher = null;
        scanCtx.setStartTime(System.currentTimeMillis());
        int key = ContextManager.setContext(scanCtx);
        boolean hasPagination = false;
        try {
          synchronized (OJVMDirectory.class) {
            Entry entry = OJVMDirectory.getCachedDirectory(dir);
            Parameters pars = entry.getDirectory().getParameters();
            // restore LogLevel
            Level lvl = Level.parse(pars.getParameter("LogLevel", "WARNING"));
            logger.setLevel(lvl);
            scanCtx.setLogLevel(lvl);
            String columnName = pars.getParameter("DefaultColumn");
            boolean trackMaxScore = false;
            boolean docsScoredInOrder =
                "true".equalsIgnoreCase(pars.getParameter("PreserveDocIdOrder",
                                                          "false"));
            if (storeScore)
                trackMaxScore =
                        "true".equalsIgnoreCase(pars.getParameter("NormalizeScore",
                                                                  "true"));
            if (logger.isLoggable(Level.INFO)) {
              logger.info("Getting caching info entry: " + entry + " dir: " +
                          entry.getDirectory());
              logger.info("Indexing column: '" + columnName + "'");
              logger.info("Analyzer: " + entry.getAnalyzer());
              logger.info("trackMaxScore: " + trackMaxScore +
                          " docsScoredInOrder: " + docsScoredInOrder);
            }
            QueryParser parser = entry.getParser();
            String queryString = cmpval.trim();
            if (queryString.startsWith("rownum:[")) {
                int pos = queryString.indexOf(" AND ");
                if (pos < 0) {
                    RuntimeException t =
                        new RuntimeException("Invalid rownum syntax in lcontains() operator, can not find AND conector syntax is 'rownum:[nn TO mm] AND restOfQueryParserSyntax'");
                    logger.throwing(CLASS_NAME, "createContext", t);
                    throw t;
                }
                String rowNumInfo = queryString.substring(0, pos);
                queryString = queryString.substring(pos + 5);
                pos = rowNumInfo.indexOf(" TO ");
                if (pos < 0) {
                    RuntimeException t =
                        new RuntimeException("Invalid rownum syntax in lcontains() operator, no rownum:[nn TO mm] syntax");
                    logger.throwing(CLASS_NAME, "createContext", t);
                    throw t;
                }
                startIndex =
                        Integer.parseInt(rowNumInfo.substring(8, pos).trim()) -
                        1;
                endIndex =
                        Integer.parseInt(rowNumInfo.substring(pos + 4, rowNumInfo.length() -
                                                              1).trim()) - 1;
                if (endIndex < 0 || startIndex < 0) { // Sanity checks
                    RuntimeException t =
                        new RuntimeException("Invalid rownum syntax in lcontains() operator, index can not be less than 1");
                    logger.throwing(CLASS_NAME, "createContext", t);
                    throw t;
                }
                if (endIndex < startIndex) {
                    RuntimeException t =
                        new RuntimeException("Invalid rownum syntax in lcontains() operator, end index is less than begin index");
                    logger.throwing(CLASS_NAME, "createContext", t);
                    throw t;
                }
                if (logger.isLoggable(Level.INFO)) {
                  logger.info("from: '" + startIndex + "'");
                  logger.info("to: '" + endIndex + "'");
                  logger.info("queryString: '" + queryString + "'");
                }
                hasPagination = true;
            }
            Query qry = parser.parse(queryString);
            qryStr =
                    sort + "(" + qry.toString(columnName) + ")" + ((storeScore) ?
                                                                   "Score" :
                                                                   "noScore");
            searcher = entry.getSeacher();
            if (logger.isLoggable(Level.INFO))
              logger.info("searcher: " + searcher.hashCode() + " qryStr: " +
                          qryStr);
            hits = entry.getHits(qryStr);
            TopFieldCollector tfc = null;
            if (hits == null) { // is new query using this query string
                String filterKeyStr = qry.toString(columnName);
                Filter docsFilter = entry.getFilter(filterKeyStr);
                if (docsFilter == null) {
                    docsFilter = new QueryWrapperFilter(qry);
                    entry.addFilter(filterKeyStr, docsFilter);
                    if (logger.isLoggable(Level.INFO))
                      logger.info("storing cachingFilter: " + docsFilter +
                                  " key: " + filterKeyStr);
                }
                if (hasPagination && firstRowHint) { // getting all hits
                    // up to endIndex
                    tfc = TopFieldCollector.create(getSort(sort), endIndex + 1, false, storeScore,
                          trackMaxScore, docsScoredInOrder);
                    searcher.search(qry, docsFilter, tfc);
                    hits = tfc.topDocs();
                    logger.info("searching with pagination and FIRST_ROW hint, getting " +
                                (endIndex + 1) +
                                " hits, hits.scoreDocs.length=" +
                                hits.scoreDocs.length + " using: " + tfc);
                    endIndex =
                            (endIndex >= hits.totalHits) ? hits.totalHits - 1 :
                            endIndex;
                } else { // getting all hits upto 2000 (oracle ODCI batch size)
                    tfc = TopFieldCollector.create(getSort(sort), (endIndex >= 2000) ? endIndex + 1 :
                                         2000, false, storeScore,
                          trackMaxScore, docsScoredInOrder);
                    searcher.search(qry, docsFilter, tfc);
                    hits = tfc.topDocs();
                    logger.info("searching without FIRST_ROW or pagination information, getting " +
                                ((endIndex >= 2000) ? endIndex + 1 : 2000) +
                                " hits, hits.scoreDocs.length=" +
                                hits.scoreDocs.length + " using: " + tfc);
                    if (hasPagination) { // if endIndex is bigger than total
                        // hits resize it
                        endIndex =
                                (endIndex >= hits.totalHits) ? hits.totalHits -
                                1 : endIndex;
                    } else { // adjust endIndex to totalHits-1
                        endIndex = hits.totalHits - 1;
                    }
                }
                if (hits.scoreDocs.length < endIndex) {
                    // if no pagination information is given, may be 2000 is not
                    // all hits, refecth all
                    tfc = TopFieldCollector.create(getSort(sort), endIndex + 1, false, storeScore,
                          trackMaxScore, docsScoredInOrder);
                    searcher.search(qry, docsFilter, tfc);
                    hits = tfc.topDocs();
                    logger.info("refetching hits, getting " + (endIndex + 1) +
                                " hits, hits.scoreDocs.length=" +
                                hits.scoreDocs.length + " using: " + tfc);
                }
                entry.setHits(qryStr, hits);
                if (logger.isLoggable(Level.INFO))
                  logger.info("storing cachingHits: " + hits.hashCode() +
                              " on the entry: " + entry.hashCode() +
                              " qryStr: " + qryStr);
            } else {
                if (hasPagination) {
                    endIndex =
                            (endIndex >= hits.totalHits) ? hits.totalHits - 1 :
                            endIndex;
                    logger.info("using pagination, found a cached set of hits, endIndex=" +
                                endIndex);
                } else {
                    endIndex = hits.totalHits - 1;
                    logger.info("not using pagination, found a cached set of hits, endIndex=" +
                                endIndex);
                }

                if (hits.scoreDocs.length <= endIndex) {
                    // May be cached hits is not enough for this window, refetch
                    // upto endIndex+1
                    String filterKeyStr = qry.toString(columnName);
                    Filter docsFilter = entry.getFilter(filterKeyStr);
                    tfc = TopFieldCollector.create(getSort(sort), endIndex + 1, false, storeScore,
                          trackMaxScore, docsScoredInOrder);
                    searcher.search(qry, docsFilter, tfc);
                    hits = tfc.topDocs();
                    entry.setHits(qryStr, hits);
                    if (logger.isLoggable(Level.INFO)) {
                      logger.info("refetching hits, getting " + (endIndex + 1) +
                                  " hits, fetched hits.scoreDocs.length=" +
                                  hits.scoreDocs.length + " using: " + tfc);
                      logger.info("storing cachingHits: " + hits.hashCode() +
                                  " on the entry: " + entry.hashCode() +
                                  " qryStr: " + qryStr);
                    }
                }
            }
            scanCtx.setQuery(qry);
            scanCtx.setStartIndex(startIndex);
            scanCtx.setEndIndex(endIndex);
            scanCtx.setHits(hits);
            scanCtx.setDir(dir);
            if (storeScore) {
                scanCtx.setScoreList(new Hashtable(hits.totalHits));
                scanCtx.setStoreScore(storeScore);
            }
            if (hits.totalHits > 0 && hits.getMaxScore() > 1.0f &&
                trackMaxScore) {
                scanCtx.setScoreNorm(1.0f / hits.getMaxScore());
            } else {
                scanCtx.setScoreNorm(Float.NaN);
            }
          }
        } catch (SQLException s) {
            RemoteException t =
                new RemoteException("IndexScanImpl error in start: " + dir, s);
            logger.throwing(CLASS_NAME, "start", t);
            throw t;
        } catch (IOException e) {
            RemoteException t =
                new RemoteException("IndexScanImpl error in start: " + dir, e);
            logger.throwing(CLASS_NAME, "start", t);
            throw t;
        } catch (ParseException p) {
            RemoteException t =
                new RemoteException("IndexScanImpl error in start: " + dir, p);
            logger.throwing(CLASS_NAME, "start", t);
            throw t;
        }
        //logger.exiting(CLASS_NAME, "start", key);
        return key;
    }

    /**
     * Fetch next nrows from an specific context
     * @param ctx scan context
     * @param nrows numbers of hits to be consumed
     * @return an array of int which are Lucene docId
     * @throws RemoteException
     */
    public String[] fetch(int ctx, BigDecimal nrows) throws RemoteException {
        //logger.entering(CLASS_NAME, "fecth", new Object[] { ctx, nrows });
        ScanContext scanCtx;
        scanCtx = (ScanContext)ContextManager.getContext(ctx);
        logger.setLevel(scanCtx.getLogLevel());
        String rid;
        boolean done = false;
        int nRows = nrows.intValue();
        String[] rlist = new String[nRows];
        Hashtable slist = null;
        boolean storeScore = scanCtx.isStoreScore();
        // Gets pre-computed hits
        TopDocs hits = scanCtx.getHits();
        if (storeScore)
            slist = scanCtx.getScoreList();
        // Gets current windows, specially for pagination
        int startIndex = scanCtx.getStartIndex();
        int endIndex = scanCtx.getEndIndex();
        // System.out.println(".ODCIFetch storeScore: " + storeScore);
        // System.out.println(".ODCIFetch firstRows: " + firstRows);
        // ***************
        // Fetch rowids *
        // ***************
        if (logger.isLoggable(Level.INFO))
          logger.info("startIndex= " + startIndex + " endIndex= " + endIndex +
                      " hits.scoreDocs.length= " + hits.scoreDocs.length +
                      " this: " + this);
        synchronized (OJVMDirectory.class) {
            Entry entry;
            try {
                entry = OJVMDirectory.getCachedDirectory(scanCtx.getDir());
            } catch (IOException e) {
              RemoteException t =
                  new RemoteException("IndexScanImpl error in fetch: " + scanCtx.getDir(), e);
              logger.throwing(CLASS_NAME, "fetch", t);
              throw t;
            } catch (SQLException e) {
              RemoteException t =
                  new RemoteException("IndexScanImpl error in fetch: " + scanCtx.getDir(), e);
              logger.throwing(CLASS_NAME, "fetch", t);
              throw t;
            }
            for (int i = 0; !done && i < nRows; i++) {
                //System.out.println("i="+i);
                if (startIndex <= endIndex) {
                    // append rowid to collection
                    try {
                        int doc = hits.scoreDocs[startIndex].doc;
                        rid = entry.getRowId(doc);
                        //logger.fine("getting doc: "+doc+" rowid="+rid);
                        if (storeScore)
                            slist.put(rid, new Integer(startIndex));
                        rlist[i] = rid;
                        startIndex++;
                    } catch (IOException e) {
                      RemoteException t =
                          new RemoteException("IndexScanImpl error in fetch: " + scanCtx.getDir(), e);
                      logger.throwing(CLASS_NAME, "fetch", t);
                      throw t;
                    }
                } else {
                    // append null rowid to collection, signal Oracle that there is
                    // no more rowids
                    rlist[i] = null;
                    done = true;
                }
            }
        }
        // update current position
        scanCtx.setStartIndex(startIndex);
        //logger.exiting(CLASS_NAME, "fetch", rlist);
        return rlist;
    }

    /**
     * close/free the scan context
     * if it has a list of score/rowid mappings stored also free this list
     * @param ctx context which is traversed
     * @throws RemoteException
     */
    public void close(int ctx) throws RemoteException {
        //logger.entering(CLASS_NAME, "close", new Object[] { ctx });
        ScanContext sctx = (ScanContext)ContextManager.clearContext(ctx);
        logger.setLevel(sctx.getLogLevel());
        if (sctx.isStoreScore()) { // storage of rowids
            sctx.getScoreList().clear();
            sctx.setScoreList(null);
        }
        long elapsedTime = System.currentTimeMillis() - sctx.getStartTime();
        logger.info("Elapsed time: " + elapsedTime + " millisecond.");
        //logger.exiting(CLASS_NAME, "close");
    }

    /**
     * get a Lucene Query without LDI extension syntax, for example without
     * [rownum:....] AND extension
     * call this function only after start method
     * @param ctx scan context
     * @return a Lucene Query
     * @throws RemoteException
     */
    public Query getQuery(int ctx) throws RemoteException {
        //logger.entering(CLASS_NAME, "getQuery", new Object[] { ctx });
        ScanContext scanCtx;
        scanCtx = (ScanContext)ContextManager.getContext(ctx);
        Query qry = scanCtx.getQuery();
        //logger.exiting(CLASS_NAME, "getQuery", qry);
        return qry;
    }

    /**
     * return the score list mapping (rowid<->score) association to be used by
     * lscore() operator
     * @param ctx scan context which have this score list
     * @return a Hashtable with the mapping of rowid->score value
     * @throws RemoteException
     */
    public Hashtable getScoreList(int ctx) throws RemoteException {
        //logger.entering(CLASS_NAME, "getScoreList", new Object[] { ctx });
        ScanContext scanCtx;
        scanCtx = (ScanContext)ContextManager.getContext(ctx);
        Hashtable scoreList = scanCtx.getScoreList();
        //logger.exiting(CLASS_NAME, "getScoreList", scoreList);
        return scoreList;
    }

    /**
     * @param ctx scan context
     * @return TopDocs (hits) of this scan
     * @throws RemoteException
     */
    public TopDocs getHits(int ctx) throws RemoteException {
        //logger.entering(CLASS_NAME, "getHits", new Object[] { ctx });
        ScanContext scanCtx;
        scanCtx = (ScanContext)ContextManager.getContext(ctx);
        TopDocs hits = scanCtx.getHits();
        //logger.exiting(CLASS_NAME, "getHits", hits);
        return hits;
    }

    /**
     * @param ctx scan context
     * @param docId Lucene doc id
     * @return a BigDecimal value with score computed during start operation
     * @throws RemoteException
     */
    public BigDecimal getScoreValue(int ctx, int docId) throws RemoteException {
        //logger.entering(CLASS_NAME, "getScoreValue", new Object[] { ctx, docId });
        ScanContext scanCtx;
        scanCtx = (ScanContext)ContextManager.getContext(ctx);
        TopDocs hits;
        hits = scanCtx.getHits();
        //logger.info("getScoreValue: " + hits.scoreDocs[docId].score);
        float scoreNorm = scanCtx.getScoreNorm();
        if (Float.isNaN(scoreNorm))
           return new BigDecimal(hits.scoreDocs[docId].score);
        else
           return new BigDecimal(hits.scoreDocs[docId].score * scoreNorm);
    }

    /**
     * method used to refresh LDI per process cache for example
     * parameters cache or LDI structures, if an index is dropped or altered
     * the specific LDI caches should be refreshed
     * @throws RemoteException
     */
    public void refreshCache(String dir) throws RemoteException {
        //logger.entering(CLASS_NAME, "refreshCache", new Object[] { dir });
        try {
          synchronized (OJVMDirectory.class) {
                OJVMDirectory.invalidateCachedEntry(dir);
                Parameters.refreshCache(dir);
          }
        } catch (IOException e) {
          logger.warning("IndexScanImpl error in refreshCache directory: " + dir + " error is: " + e.getLocalizedMessage());
        }
        //logger.exiting(CLASS_NAME, "refreshCache");
    }

    /**
     * Compute a number of hist for given Lucene Query, also caches the query as
     * filter to be re-used by another countHist or lcontains operation
     * @param dir LDI directory, SCHEMA.IDX syntax
     * @param cmpval Lucene Query Syntax
     * @return an int with the number of hist
     * @throws RemoteException
     */
    public int getNumHits(String dir, String cmpval) throws RemoteException {
        //logger.entering(CLASS_NAME, "countHits", new Object[] { dir, cmpval });
        try {
            synchronized (OJVMDirectory.class) {
                Entry entry = OJVMDirectory.getCachedDirectory(dir);
                Parameters parameters = entry.getDirectory().getParameters();
                logger.setLevel(Level.parse(parameters.getParameter("LogLevel",
                                                                    "WARNING")));
                String columnName = parameters.getParameter("DefaultColumn");
                QueryParser parser = entry.getParser();
                String queryString = cmpval.trim();
                Query qry = parser.parse(queryString);
                String filterKeyStr = qry.toString(columnName);
                Filter docsFilter = entry.getFilter(filterKeyStr);
                if (docsFilter == null) {
                    docsFilter = new QueryWrapperFilter(qry);
                    entry.addFilter(filterKeyStr, docsFilter);
                    if (logger.isLoggable(Level.INFO))
                        logger.info("storing cachingFilter: " + docsFilter +
                                    " key: " + filterKeyStr);
                }
                IndexSearcher searcher = entry.getSeacher();
                CountHitCollector hitCollector = new CountHitCollector(0);
                searcher.search(qry, hitCollector);
                int totalHits = hitCollector.getNumHits();
                //logger.exiting(CLASS_NAME, "countHits", totalHits);
                return totalHits;
            }
        } catch (SQLException s) {
          RemoteException t =
              new RemoteException("IndexScanImpl error in countHits: " + dir, s);
          logger.throwing(CLASS_NAME, "countHits", t);
          throw t;
        } catch (IOException e) {
          RemoteException t =
              new RemoteException("IndexScanImpl error in countHits: " + dir, e);
          logger.throwing(CLASS_NAME, "countHits", t);
          throw t;
        } catch (ParseException p) {
          RemoteException t =
              new RemoteException("IndexScanImpl error in countHits: " + dir, p);
          logger.throwing(CLASS_NAME, "countHits", t);
          throw t;
        }
    }

    /**
     * Compute a Lucene Sort object based on a sort string ASC means Score
     * reverse DESC means natural lucene sorting, score descending
     * col[:direction[:type]],.. sort by columns col is column name stored in
     * Lucene Index, note that master index columns usually are capitalized
     * direction is ASC or DESC, if its ommited default is ASC type is int,
     * string, float or auto; if its ommited default is auto
     *
     * @param sortStr
     *            using above syntax
     * @return Sort Lucene object
     */
    public Sort getSort(String sortStr) {
        Sort sort = new Sort();
        if (sortStr.startsWith("ASC") || sortStr.startsWith("DESC")) { // short
            // format
            if (sortStr.startsWith("ASC"))
                sort.setSort(new SortField[] { new SortField(null,
                                                             SortField.Type.SCORE,
                                                             true) });
        } else {
            String[] sortFields = sortStr.split(",");
            SortField[] sortFieldParam = new SortField[sortFields.length];
            for (int i = 0; i < sortFields.length; i++) {
                String[] sortParams = sortFields[i].split(":");
                SortField.Type sortType = SortField.Type.STRING;
                boolean reverse = false;
                if (sortParams.length == 0)
                    throw new RuntimeException("Invalid sort field syntax, must be lconstains(col,'text to seatch','column:[string|float|int]:[ASC|DESC],...')");
                else if (sortParams.length == 1) {
                    reverse = false;
                } else if (sortParams.length == 2) {
                    reverse =
                            (sortParams[1].equalsIgnoreCase("DESC") ? true : false);
                } else if (sortParams.length == 3) {
                    reverse =
                            (sortParams[1].equalsIgnoreCase("DESC") ? true : false);
                    sortType =
                            sortParams[2].equalsIgnoreCase("string") ? SortField.Type.STRING :
                            (sortParams[2].equalsIgnoreCase("int") ?
                             SortField.Type.INT :
                             (sortParams[2].equalsIgnoreCase("float") ?
                              SortField.Type.FLOAT : SortField.Type.STRING));
                } else
                    throw new RuntimeException("Invalid sort field syntax, must be lconstains(col,'text to seatch','column:[string|float|int]:[ASC|DESC],...')");
                sortFieldParam[i] =
                        new SortField(sortParams[0], sortType, reverse);
            }
            sort.setSort(sortFieldParam);
        }
        logger.info("using sort: " + sort);
        return sort;
    }
}
