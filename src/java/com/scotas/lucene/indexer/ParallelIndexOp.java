package com.scotas.lucene.indexer;

import java.io.IOException;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.ODCI.ODCIRidList;

import oracle.jdbc.OracleCallableStatement;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import com.scotas.lucene.store.OJVMDirectory;
import com.scotas.lucene.store.OJVMUtil;
import org.apache.lucene.store.RAMDirectory;
import com.scotas.lucene.util.StringUtils;

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


public class ParallelIndexOp {
  /**
   * Java Util Logging variables and default values
   */
  private static Logger logger = null;

  /**
   * Constant used to get Logger name
   */
  static final String CLASS_NAME = ParallelIndexOp.class.getName();

  static {
    logger = Logger.getLogger(CLASS_NAME);
    // default Log level, override it using LuceneDomainIndex.setLogLevel('level')
    logger.setLevel(Level.WARNING);
  }

  public ParallelIndexOp() {
    super();
  }

  /**
   * Process changes from Lucene Domain Index queue working in SyncMode:OnLine
   * insert operation will be stored in tmpIdx index to finally merged on prefix
   * directory
   * This operation can be executed in parallel without prefix dir blocked
   * @param tmpIdx temporary storage
   * @param prefix master index storage
   * @param inserted list of rowids being inserted
   * @throws IOException
   * @throws SQLException
   */
  public static void addDocToTempIdx(String tmpIdx, String prefix,
                                     ODCIRidList inserted) throws IOException,
                                                                  SQLException {
    if (inserted == null || inserted.length() == 0) // Sanity checks
      return; // Shorcut ;)
    logger.entering(CLASS_NAME, "addDocToTempIdx",
                    new Object[] { tmpIdx, prefix, inserted });
    IndexWriter writer = null;
    OJVMDirectory dir = null;
    Directory dirs[] = new Directory[1];
    OJVMDirectory tmpDir = null;
    Connection conn = OJVMUtil.getConnection();
    try {
      long elapsedTime = System.currentTimeMillis();
      tmpDir = OJVMDirectory.getDirectory(tmpIdx);
      Parameters par = tmpDir.getParameters();
      writer = LuceneDomainIndex.getIndexWriterForDir(tmpDir, true);
      String logLevel = par.getParameter("LogLevel", "WARNING");
      logger.setLevel(Level.parse(logLevel)); // Restore log level
      String col = par.getParameter("ColName");
      String tableSchema = par.getParameter("TableSchema");
      String tableName = par.getParameter("TableName");
      String partition = par.getParameter("Partition");
      UserDataStore userDataStore = LuceneDomainIndex.getUserDataStore(par);
      boolean includeMasterColumn =
        "true".equalsIgnoreCase(par.getParameter("IncludeMasterColumn",
                                                 "true"));
      String extraCols = par.getParameter("ExtraCols");
      String extraTabs = par.getParameter("ExtraTabs");
      String whereCondition = par.getParameter("WhereCondition");
      boolean lockMasterTable =
          "true".equalsIgnoreCase(par.getParameter("LockMasterTable", "true"));
      TableIndexer index =
        new TableIndexer(conn, tableSchema, tableName,
                         partition);
      index.setUserDataStore(userDataStore);
      index.setExtraColsStr(extraCols);
      index.setExtraTabsStr(extraTabs);
      index.setExtraWhereStr(whereCondition);
      index.setLockMasterTable(lockMasterTable);
      logger.info(".addDocToTempIdx - start indexing on " + tmpIdx +
                  " numRows= " + inserted.length());
      index.index(logger, writer, col, inserted.getArray(),
                  includeMasterColumn);
      writer.commit();
      writer.waitForMerges();
      writer.close();
      writer = null;
      elapsedTime = System.currentTimeMillis() - elapsedTime;
      dirs[0] = tmpDir;
      logger.info(".addDocToTempIdx - indexing done " + tmpIdx +
                  " elapsedTime: " + elapsedTime + " ms.");
      OracleCallableStatement cs = null;
      try {
        elapsedTime = System.currentTimeMillis();
        cs =
            (OracleCallableStatement)conn.prepareCall(StringUtils.replace(LuceneDomainIndex.lckLuceneTableStmt,
                                                                          "%IDX%",
                                                                          prefix));
        cs.execute();
        dir = OJVMDirectory.getDirectory(prefix);
        writer = LuceneDomainIndex.getIndexWriterForDir(dir, false);
        // Do merge
        logger.info(".addDocToTempIdx - addIndexesNoOptimize starting merge on " +
                    prefix + " using OJVMDirectory: " + dirs[0]);
        //TODO: lock for info stream functionality in 4.0.0
        //if (logger.isLoggable(Level.INFO))
        //    writer.setInfoStream(System.out);
        writer.addIndexes(dirs);
        writer.commit();
        writer.waitForMerges();
        writer.close();
        writer = null;
        elapsedTime = System.currentTimeMillis() - elapsedTime;
        logger.info(".addDocToTempIdx - addIndexesNoOptimize merge done " +
                    prefix + " elapsedTime: " + elapsedTime + " ms.");
        // after sync purge deleted documents
        dir.purge();
        conn.commit();
      } finally {
                OJVMUtil.closeDbResources(cs, null);
      }
    } catch (SQLException e) {
      RuntimeException t =
        new RuntimeException("Error addDocToTempIdx - index: " + " - " +
                             tmpIdx + " - " + e.getMessage());
      logger.throwing(CLASS_NAME, "addDocToTempIdx", t);
      throw t;
    } finally {
      if (writer != null)
        writer.close();
      writer = null;
      if (tmpDir != null)
        tmpDir.close();
      tmpDir = null;
      if (dir != null)
        dir.close();
      dir = null;
    }
  }

  /**
   * Process changes from Lucene Domain Index queue working in SyncMode:OnLine
   * insert operation will be stored in a RAMDirectory to finally merged on prefix
   * directory
   * This operation can be executed in parallel without prefix dir blocked
   * @param prefix master index storage
   * @param inserted batch of rowids being inserted
   * @throws IOException
   * @throws SQLException
   */
  public static void addDocToIdx(String prefix,
                                 ODCIRidList inserted) throws IOException,
                                                              SQLException {
    if (inserted == null || inserted.length() == 0) // Sanity checks
      return; // Shorcut ;)
    logger.entering(CLASS_NAME, "addDocToIdx",
                    new Object[] { prefix, inserted });
    IndexWriter writer = null;
    OJVMDirectory dir = null;
    Directory dirs[] = new Directory[1];
    Connection conn = null;
    try {
      long elapsedTime = System.currentTimeMillis();
      dirs[0] = new RAMDirectory();
      dir = OJVMDirectory.getDirectory(prefix);
      conn = dir.getConnection();
      Parameters par = dir.getParameters();
      String logLevel = par.getParameter("LogLevel", "WARNING");
      logger.setLevel(Level.parse(logLevel)); // Restore log level
      logger.info(".addDocToIdx - start indexing on " + prefix + " numRows= " +
                  inserted.length());
      String col = par.getParameter("ColName");
      String tableSchema = par.getParameter("TableSchema");
      String tableName = par.getParameter("TableName");
      String partition = par.getParameter("Partition");
      Analyzer analyzer = LuceneDomainIndex.getAnalyzer(par);
      IndexWriterConfig ic = new IndexWriterConfig(LuceneDomainIndex.LUCENE_COMPAT_VERSION,analyzer);
      
      ic.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
      writer =
          new IndexWriter(dirs[0], ic);
      //TODO: lock for info stream functionality in 4.0.0
      //if (logger.isLoggable(Level.INFO))
      //    writer.setInfoStream(System.out);
      UserDataStore userDataStore = LuceneDomainIndex.getUserDataStore(par);
      boolean includeMasterColumn =
        "true".equalsIgnoreCase(par.getParameter("IncludeMasterColumn",
                                                 "true"));
      String extraCols = par.getParameter("ExtraCols");
      String extraTabs = par.getParameter("ExtraTabs");
      String whereCondition = par.getParameter("WhereCondition");
      boolean lockMasterTable =
          "true".equalsIgnoreCase(par.getParameter("LockMasterTable", "true"));
      TableIndexer index =
        new TableIndexer(conn, tableSchema, tableName,
                         partition);
      index.setUserDataStore(userDataStore);
      index.setExtraColsStr(extraCols);
      index.setExtraTabsStr(extraTabs);
      index.setExtraWhereStr(whereCondition);
      index.setLockMasterTable(lockMasterTable);
      index.index(logger, writer, col, inserted.getArray(),
                  includeMasterColumn);
      writer.commit();
      writer.waitForMerges();
      writer.close();
      writer = null;
      elapsedTime = System.currentTimeMillis() - elapsedTime;
      logger.info(".addDocToIdx - indexing done " + prefix + " elapsedTime: " +
                  elapsedTime + " ms.");
      OracleCallableStatement cs = null;
      try {
        elapsedTime = System.currentTimeMillis();
        cs =
            (OracleCallableStatement)conn.prepareCall(StringUtils.replace(LuceneDomainIndex.lckLuceneTableStmt,
                                                                          "%IDX%",
                                                                          prefix));
        cs.execute();
        writer = LuceneDomainIndex.getIndexWriterForDir(dir, false);
        // Do merge
        logger.info(".addDocToIdx - addIndexesNoOptimize starting merge on " +
                    prefix + " using RAMDirectory: " + dirs[0]);
        writer.addIndexes(dirs);
        writer.commit();
        writer.waitForMerges();
        writer.close();
        writer = null;
        elapsedTime = System.currentTimeMillis() - elapsedTime;
        logger.info(".addDocToIdx - addIndexesNoOptimize merge done " +
                    prefix + " elapsedTime: " + elapsedTime + " ms.");
        // after sync purge deleted documents
        dir.purge();
        conn.commit();
      } finally {
                OJVMUtil.closeDbResources(cs, null);
      }
    } catch (SQLException e) {
      RuntimeException t =
        new RuntimeException("Error addDocToIdx - index: " + " - " + prefix +
                             " - " + e.getMessage());
      logger.throwing(CLASS_NAME, "addDocToIdx", t);
      throw t;
    } finally {
      if (writer != null) {
        writer.close();
        writer = null;
      }
      if (dirs[0] != null) {
        dirs[0].close();
        dirs[0] = null;
      }
      if (dir != null)
        dir.close();
      dir = null;
    }
  }
}
