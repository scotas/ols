package com.scotas.lucene.search.similar.ojvm;

import java.io.IOException;

import java.io.StringReader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.ODCI.ODCIRidList;

import oracle.sql.CLOB;

import oracle.xdb.XMLType;

import org.apache.lucene.index.IndexReader;
import com.scotas.lucene.indexer.Entry;
import com.scotas.lucene.indexer.Parameters;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollector;
import com.scotas.lucene.store.OJVMDirectory;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import com.scotas.lucene.store.OJVMUtil;

import java.io.Reader;

import org.apache.lucene.search.IndexSearcher;

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

public class MoreLike {
    /**
     * Constant used to get Logger name
     */
    static final String CLASS_NAME = "org.apache.lucene.search.similar.ojvm.MoreLike";

    public MoreLike() {
    }

    public static ODCIRidList getSimilar(String prefix, String rowid,
                                         java.math.BigDecimal fromRow,
                                         java.math.BigDecimal toRow,
                                         java.math.BigDecimal minTermFreq,
                                         java.math.BigDecimal minDocFreq) throws IOException,
                                                                            SQLException {
        Logger logger = Logger.getLogger(CLASS_NAME);
        ResultSet resultset = null;
        PreparedStatement preparedstatement = null;
        Query qry = null;
        Entry entry;
        String[] rids = null;
        entry = OJVMDirectory.getCachedDirectory(prefix);
        Parameters parameters = entry.getDirectory().getParameters();
        String logLevel = parameters.getParameter("LogLevel","WARNING");
        logger.setLevel(Level.parse(logLevel)); // Restore log level
        logger.entering(CLASS_NAME, "getSimilar",
                        new Object[] { prefix, rowid, fromRow, toRow });
        String columnName = parameters.getParameter("DefaultColumn");
        logger.info("Indexing column: '" + columnName + "'");
        logger.info("Analyzer: " + entry.getAnalyzer());
        IndexReader ir = entry.getReader();
        MoreLikeThis mlt = new MoreLikeThis(ir);
        mlt.setAnalyzer(entry.getAnalyzer());
        mlt.setMinTermFreq(minTermFreq.intValue());
        mlt.setMinDocFreq(minDocFreq.intValue());
        logger.info(mlt.toString());
        try {
            Connection conn = entry.getDirectory().getConnection();
            preparedstatement = conn.prepareStatement(getStmt(parameters));
            preparedstatement.setString(1, rowid);
            resultset = preparedstatement.executeQuery();
            if (resultset.next()) {
                Object mltResult = resultset.getObject(columnName);
                if (mltResult instanceof XMLType)
                    qry = mlt.like(((XMLType)mltResult).getCharacterStream(), columnName);
                
                else if (mltResult instanceof CLOB)
                    qry = mlt.like(((CLOB)mltResult).getCharacterStream(), columnName);
                else
                    qry = mlt.like(new StringReader(mltResult.toString()), columnName);
            } else {
                RuntimeException t =
                    new RuntimeException("Can not get a more like this text for this rowid: " +
                                         rowid);
                logger.throwing(CLASS_NAME, "getSimilar", t);
                throw t;
            }
            if (qry.toString(columnName).length() == 0) {
                ODCIRidList rList = new ODCIRidList(rids);
                logger.exiting(CLASS_NAME, "getSimilar", rList);
                return rList;
            }
            String qryStr = "DESC(" + qry.toString(columnName) + ")noScore";
            logger.info("qryStr: " + qryStr);
            IndexSearcher searcher = entry.getSeacher();
            TopDocs hits = entry.getHits(qryStr);
            TopFieldCollector tfc = null;
            boolean storeScore = false;
            boolean trackMaxScore = false;
            boolean docsScoredInOrder = "true".equalsIgnoreCase(parameters.getParameter("PreserveDocIdOrder","false"));
            logger.info("trackMaxScore: " + trackMaxScore + " docsScoredInOrder: " + docsScoredInOrder);
            if (hits == null) { // is new query using this parser
                String filterKeyStr = qry.toString(columnName);
                Filter docsFilter = entry.getFilter(filterKeyStr);
                if (docsFilter == null) {
                  docsFilter = new QueryWrapperFilter(qry);
                  entry.addFilter(filterKeyStr, docsFilter);
                  logger.info("storing cachingFilter: " + docsFilter);
                }
                tfc = TopFieldCollector.create(Sort.RELEVANCE,toRow.intValue(),false,storeScore,trackMaxScore,docsScoredInOrder);
                //hits = searcher.search(qry, docsFilter, toRow.intValue());
                searcher.search(qry,docsFilter,tfc);
                hits = tfc.topDocs();
                entry.setHits(qryStr, hits);
                logger.info("fetching hits endIndex:" + toRow.intValue());
                logger.info("storing cachingHits: " + hits.hashCode() +
                            " on the entry: " + entry.hashCode() +
                            " qryStr: " + qryStr);
            } else {
                logger.info("using cachingHits: " + hits.hashCode() +
                            " on the entry: " + entry.hashCode() +
                            " qryStr: " + qryStr);
                if (hits.scoreDocs.length<toRow.intValue()) {
                  // May be cached hits is not enough for this window, refetch upto endIndex
                  String filterKeyStr = qry.toString(columnName);
                    Filter docsFilter = entry.getFilter(filterKeyStr);
                  //hits = searcher.search(qry, docsFilter, toRow.intValue());
                  tfc = TopFieldCollector.create(Sort.RELEVANCE,toRow.intValue(),false,storeScore,trackMaxScore,docsScoredInOrder);
                  searcher.search(qry,docsFilter,tfc);
                  hits = tfc.topDocs();
                  entry.setHits(qryStr, hits);
                  logger.info("refetching hits endIndex:" + toRow.intValue());
                  logger.info("storing cachingHits: " + hits.hashCode() +
                              " on the entry: " + entry.hashCode() +
                              " qryStr: " + qryStr);
                }
            }
            if (hits.totalHits == 0) {
                rids = new String[1];
                rids[0] = rowid; // by default this rowid is like himself
                ODCIRidList rList = new ODCIRidList(rids);
                logger.exiting(CLASS_NAME, "getSimilar", rList);
                return rList;
            }
            int fromHit =
                (fromRow.intValue() > hits.totalHits) ? hits.totalHits :
                fromRow.intValue() - 1;
            int toHit =
                (toRow.intValue() > hits.totalHits) ? hits.totalHits : toRow.intValue() -
                1;
            if (fromHit > toHit) { // Sanity checks
                RuntimeException t =
                    new RuntimeException("fromHit (" + fromHit +
                                         ") can not be greather than toHit (" +
                                         toHit + ")");
                logger.throwing(CLASS_NAME, "getSimilar", t);
                throw t;
            }
            logger.info("iterating from: " + fromHit + " to: " + toHit + " hits.scoreDocs.length= " + hits.scoreDocs.length);
            rids = new String[(toHit - fromHit) + 1];
            for (int i = 0; i < rids.length; i++) {
                int doc = hits.scoreDocs[i + fromHit].doc;
                rids[i] = entry.getRowId(doc);
            }
        } catch (IOException e) {
            RuntimeException t = new RuntimeException(e);
            logger.throwing(CLASS_NAME, "getSimilar", t);
            throw t;
        } catch (SQLException s) {
            RuntimeException t = new RuntimeException(s);
            logger.throwing(CLASS_NAME, "getSimilar", t);
            throw t;
        } finally {
            OJVMUtil.closeDbResources(preparedstatement, resultset);
        }
        ODCIRidList rList = new ODCIRidList(rids);
        logger.exiting(CLASS_NAME, "getSimilar", rList);
        return rList;
    }

    private static String getStmt(Parameters pars) {
        boolean withMasterColumn =
            "true".equalsIgnoreCase(pars.getParameter("IncludeMasterColumn",
                                                      "true"));
        String extraColsStr = pars.getParameter("ExtraCols");
        String extraTabsStr = pars.getParameter("ExtraTabs");
        String extraWhereStr = pars.getParameter("WhereCondition");
        String schemaName = pars.getParameter("TableSchema");
        String tableName = pars.getParameter("TableName");
        String partitionName = pars.getParameter("Partition");
        String col = pars.getParameter("ColName");
        if (!withMasterColumn && extraColsStr == null) // Sanity checks
            throw new RuntimeException("Can't not use flag IncludeMasterColumn:false without ExtraCols parameter");
        StringBuffer selectStmt = new StringBuffer("SELECT ");
        selectStmt.append("/*+ DYNAMIC_SAMPLING(0) RULE NOCACHE(").append(tableName).append(") */ ");
        selectStmt.append(tableName).append(".rowid");
        if (withMasterColumn)
            selectStmt.append(",").append(tableName).append(".\"").append(col).append("\"");
        if (extraColsStr != null)
            selectStmt.append(",").append(extraColsStr);
        selectStmt.append(" FROM ").append(schemaName).append(".").append(tableName);
        if (partitionName != null && partitionName.length() > 0)
            selectStmt.append(" PARTITION (").append(partitionName).append(")");
        if (extraTabsStr != null && extraTabsStr.length() > 0)
            selectStmt.append(",").append(extraTabsStr);
        selectStmt.append(" where ");
        selectStmt.append(tableName).append(".rowid = ? ");
        if (extraWhereStr != null && extraWhereStr.length() > 0) {
            selectStmt.append("and ");
            selectStmt.append(extraWhereStr);
        }
        selectStmt.append(" for update nowait");
        //System.out.println("select stmt: " + selectStmt);
        return selectStmt.toString();
    }

}
