package com.scotas.lucene.search.facets.ojvm;

import java.io.IOException;

import java.math.BigDecimal;

import java.sql.SQLException;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.scotas.lucene.indexer.ContextManager;
import com.scotas.lucene.indexer.Entry;
import com.scotas.lucene.indexer.Parameters;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import com.scotas.lucene.search.CountHitCollector;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;

import com.scotas.lucene.store.OJVMDirectory;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.OpenBitSet;


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

/* Implementation type
 * Facets with Lucene adapted code from
 * http://sujitpal.blogspot.com/2007/04/lucene-search-within-search-with.html
 * to run as an aggregated function using ODCI API
*/
public class HitCounter {
  /**
   * Java Util Logging variables and default values
   */
  private static Logger logger = null;

  /**
   * Constant used to get Logger name
   */
  static final String CLASS_NAME = HitCounter.class.getName();

  static {
    logger = Logger.getLogger(CLASS_NAME);
    // default Log level, override it using LuceneDomainIndex.setLogLevel('level')
    logger.setLevel(Level.WARNING);
  }

  final static BigDecimal SUCCESS = new BigDecimal(0);
  final static BigDecimal ERROR = new BigDecimal(1);

  static public BigDecimal ODCIInitialize(BigDecimal[] sctx, String idx,
                                          String qry) throws SQLException,
                                                             IOException,
                                                             ParseException {
    logger.entering(CLASS_NAME, "ODCIInitialize",
                    new Object[] { sctx[0], idx, qry });
    Entry entry = OJVMDirectory.getCachedDirectory(idx);
    Parameters pars = entry.getDirectory().getParameters();
    String columnName = pars.getParameter("DefaultColumn");
    String logLevel = pars.getParameter("LogLevel", "WARNING");
    logger.setLevel(Level.parse(logLevel)); // Restore log level
    QueryParser parser = entry.getParser();
    int pos = qry.indexOf(',');
    String category = "-ALL-";
    OpenBitSet baseBitSet = null;
    if (pos > 0) {
      category = qry.substring(0, pos);
            Query qryLucene = parser.parse(category);
      String filterKeyStr = qryLucene.toString(columnName);
            Filter docsFilter = entry.getFilter(filterKeyStr);
      if (docsFilter == null) {
        docsFilter = new QueryWrapperFilter(qryLucene);
        entry.addFilter(filterKeyStr, docsFilter);
        logger.info("storing cachingFilter: " + docsFilter + " key: " + filterKeyStr );
      }
            IndexSearcher searcher = entry.getSeacher();
      CountHitCollector hitCollector = new CountHitCollector(entry.getReader().numDocs());
      searcher.search(qryLucene, hitCollector );
      baseBitSet = hitCollector.getBits();
      //logger.info("baseBitSet: " + baseBitSet);
    }
    // register stored context with cartridge services
    StoredCtx ctx =
      new StoredCtx(idx, category, entry, baseBitSet, parser, columnName);
    ctx.setStartTime(System.currentTimeMillis());
    int key;
    key = ContextManager.setContext(ctx);

    // create a Highlighter instance and store the key in it
    sctx[0] = new BigDecimal(key);
    logger.info("key '" + sctx[0] + "' main category '" + category + "'");
    logger.exiting(CLASS_NAME, "ODCIInitialize", SUCCESS);
    return SUCCESS;
  }

  static public BigDecimal ODCIIterate(BigDecimal ctx, String qry,
                                       BigDecimal[] cnt) throws SQLException,
                                                                ParseException,
                                                                IOException {
    //logger.entering(CLASS_NAME, "ODCIIterate", new Object[] { ctx, qry, cnt });
    // retrieve stored context using the key
    StoredCtx sctx;
    sctx = (StoredCtx)ContextManager.getContext(ctx.intValue());
    Entry entry = sctx.getEntry();
    String mainCategory = sctx.getQry();
    OpenBitSet baseBitSet = sctx.getBaseBitSet();
    String subCategory =
      (baseBitSet == null) ? qry : qry.substring(mainCategory.length() + 1);
    //logger.info("main category '" + mainCategory + "'");
    logger.info("sub category '" + subCategory + "'");
    QueryParser parser = sctx.getParser();
        Query qryLucene = parser.parse(subCategory);
    String filterKeyStr = qryLucene.toString(sctx.getDfltColumn());
        Filter docsFilter = entry.getFilter(filterKeyStr);
    if (docsFilter == null) {
      docsFilter = new QueryWrapperFilter(qryLucene);
      entry.addFilter(filterKeyStr, docsFilter);
      logger.info("storing cachingFilter: " + docsFilter + " key: " + filterKeyStr );
    }
    long hitCount = 0;
    if (baseBitSet != null) {
            IndexSearcher searcher = entry.getSeacher();
      CountHitCollector hitCollector =
        new CountHitCollector(entry.getReader().numDocs());
      searcher.search(qryLucene, hitCollector);
      OpenBitSet filterBitSet = hitCollector.getBits();
      filterBitSet.and(baseBitSet);
      //logger.info("filterBitSet: " + filterBitSet);
      hitCount = filterBitSet.cardinality();
    } else {
      IndexSearcher searcher = entry.getSeacher();
      CountHitCollector hitCollector = new CountHitCollector(0);
      searcher.search(qryLucene, hitCollector);
      hitCount = hitCollector.getNumHits();
    }
    cnt[0] = new BigDecimal(hitCount);
    //logger.exiting(CLASS_NAME, "ODCIIterate", SUCCESS);
    return SUCCESS;
  }

  static public BigDecimal ODCITerminate(BigDecimal ctx) throws SQLException {

    // retrieve stored context using the key, and remove from ContextManager
    logger.entering(CLASS_NAME, "ODCITerminate", new Object[] { ctx });
    StoredCtx sctx;
    int key = ctx.intValue();
    sctx = (StoredCtx)ContextManager.clearContext(key);
    long elapsedTime = System.currentTimeMillis() - sctx.getStartTime();
    logger.info("Terminating category: '" + sctx.getQry() +
                "' Elapsed time: " + elapsedTime + " millisecond.");
    logger.exiting(CLASS_NAME, "ODCITerminate", SUCCESS);
    return SUCCESS;
  }
}
