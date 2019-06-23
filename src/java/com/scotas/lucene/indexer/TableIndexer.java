package com.scotas.lucene.indexer;

import java.io.IOException;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.ODCI.ODCIRidList;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import com.scotas.lucene.store.OJVMUtil;


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

/**
 * This class is responsable for indexing a table doing a full scan or
 * re-indexing a list of rowids
 */
public class TableIndexer {
    static final String enqueueInsertChangeStmt = 
        "call LuceneDomainIndex.enqueueChange(?,?,'insert')";

    /**
     * Database connection used by this indexer
     */
    protected Connection conn;

    /**
     * Schema (database user name) owner of the table to index
     */
    protected String schemaName;

    /**
     * Table name to index
     * this string is used to make an SQL like:
     *    SELECT ROWID,extraColStr FROM tableName,extraTabStr WHERE extraWhereStr
     */
    protected String tableName;

    /**
     * Partinion to index
     */
    protected String partitionName;

    /**
     * User Defined Data Store function (control which columns are added to the index)
     */
    protected UserDataStore userDataStore;

    /**
     * Coma separated list of extra columns to index
     * this string is used to make an SQL like:
     *    SELECT ROWID,extraColStr FROM tableName,extraTabStr WHERE extraWhereStr
     */
    protected String extraColsStr;

    /**
     * Coma separated list of tables used to scan (joined to tableName)
     * this string is used to make an SQL like:
     *    SELECT ROWID,extraColStr FROM tableName,extraTabStr WHERE extraWhereStr
     */
    protected String extraTabsStr;

    /**
     * Where condition used to join above tables and columns
     * this string is used to make an SQL like:
     *    SELECT ROWID,extraColStr FROM tableName,extraTabStr WHERE extraWhereStr
     */
    protected String extraWhereStr;
    
    /**
     * Define if want to lock (for update nowait) the table to be indexes.
     * Useful for "static" tables and to parallelize the creation of multiple
     * indexes over the same table. Default value is <b>true</b>
     */
    protected boolean lockMasterTable;

    /**
     * Default empty constructor
     */
    public TableIndexer() {
        this.userDataStore = new DefaultUserDataStore();
        this.extraColsStr = null;
        this.lockMasterTable = true;
    }

    /**
     * Constructor to index a given table name and using default KRB database connection
     * @param tableName
     */
    public TableIndexer(String tableName) {
        try {
            this.conn = OJVMUtil.getConnection();
            this.tableName = tableName;
            this.schemaName =
                    this.conn.getMetaData().getUserName().toUpperCase();
            this.partitionName = null;
            this.userDataStore = new DefaultUserDataStore();
            this.userDataStore.setConnection(this.conn);
            this.extraColsStr = null;
            this.lockMasterTable = true;
        } catch (SQLException e) {
            throw new InstantiationError(e.getMessage());
        }
    }

    /**
     * Constructor for indexing a given schema.tablename
     * @param schemaName
     * @param tableName
     */
    public TableIndexer(String schemaName, String tableName) {
        try {
            this.conn = OJVMUtil.getConnection();
            this.tableName = tableName;
            this.schemaName = schemaName;
            this.partitionName = null;
            this.userDataStore = new DefaultUserDataStore();
            this.userDataStore.setConnection(this.conn);
            this.extraColsStr = null;
            this.lockMasterTable = true;
        } catch (SQLException e) {
            throw new InstantiationError(e.getMessage());
        }
    }

    /**
     * Constructor for indexing a given table name and using an specific connection
     * @param conn
     * @param tableName
     */
    public TableIndexer(Connection conn, String tableName) {
        try {
            this.conn = conn;
            this.tableName = tableName;
            this.schemaName =
                    this.conn.getMetaData().getUserName().toUpperCase();
            this.partitionName = null;
            this.userDataStore = new DefaultUserDataStore();
            this.userDataStore.setConnection(this.conn);
            this.extraColsStr = null;
            this.lockMasterTable = true;
        } catch (SQLException e) {
            throw new InstantiationError(e.getMessage());
        }
    }

    /**
     * @param conn
     * @param schemaName
     * @param tableName
     */
    public TableIndexer(Connection conn, String schemaName, String tableName) {
        this.conn = conn;
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.partitionName = null;
        this.userDataStore = new DefaultUserDataStore();
        this.userDataStore.setConnection(this.conn);
        this.extraColsStr = null;
        this.lockMasterTable = true;
    }

    /**
     * @param conn
     * @param schemaName
     * @param tableName
     * @param partitionName
     */
    public TableIndexer(Connection conn, String schemaName, String tableName,
                        String partitionName) {
        this.conn = conn;
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.partitionName = partitionName;
        this.userDataStore = new DefaultUserDataStore();
        this.userDataStore.setConnection(this.conn);
        this.extraColsStr = null;
        this.lockMasterTable = true;
    }

    /**
     * Returns a select stamenent like:
     * select T1.F2,t1.f3,t1.f4,t2.f6,t2.f7 from T1,t2 where t1.f4=t2.f5;
     * if ExtraTabs is t2 and ExtraCols is t1.f3,t1.f4,t2.f6,t2.f7
     * Note that: T1.F2 is the master table.columns of the index
     * @param col Master column of the index
     * @param passingRowids if true includes rowid pseudo-column in select
     * @param withMasterColumn if true includes master column in select
     * @return a JDBC Select statement
     */
    private String getSelectStmt(String col, boolean passingRowids, boolean withMasterColumn) {
        if (!withMasterColumn && this.extraColsStr == null) // Sanity checks
            throw new RuntimeException("Can't not use flag IncludeMasterColumn:false without ExtraCols parameter");
        StringBuffer selectStmt = new StringBuffer("SELECT ");
        selectStmt.append("/*+ DYNAMIC_SAMPLING(L$MT,0) */ ");
        selectStmt.append("L$MT").append(".rowid");
        if (withMasterColumn)
            selectStmt.append(",").append("L$MT").append(".\"").append(col).append("\"");
        if (this.extraColsStr != null)
            selectStmt.append(",").append(this.extraColsStr);
        if (this.partitionName != null && this.partitionName.length() > 0)
            selectStmt.append(" FROM ").append(schemaName).append(".").append(tableName)
                .append(" PARTITION (").append(partitionName).append(") L$MT");
        else // no partitioning information
            selectStmt.append(" FROM ").append(schemaName).append(".").append(tableName).append(" L$MT");
        if (this.extraTabsStr != null && this.extraTabsStr.length() > 0)
            selectStmt.append(",").append(this.extraTabsStr);
        if (passingRowids ||
            (this.extraWhereStr != null && this.extraWhereStr.length() > 0)) {
            selectStmt.append(" where ");
            if (passingRowids)
                //selectStmt.append(this.tableName).append(".rowid = ? ");
                selectStmt.append("L$MT").append(".rowid in (select /*+ cardinality(L$PT, 10) */ * from table(?)) ");
            if (this.extraWhereStr != null &&
                this.extraWhereStr.length() > 0) {
                if (passingRowids)
                    selectStmt.append("and (");
                selectStmt.append(this.extraWhereStr);
                if (passingRowids)
                    selectStmt.append(")");
            }
        }
        if (this.lockMasterTable)
            selectStmt.append(" for update nowait");
        //System.out.println("select stmt: " + selectStmt);
        return selectStmt.toString();
    }

    /**
     * Index a set of rowids, called from LuceneDomainIndex and ParallelIndexOp classes
     * @param writer Lucene writer
     * @param col master column of the index
     * @param rowids Set of rowid to index
     * @throws IOException
     * @see LuceneDomainIndex
     * @see ParallelIndexOp
     */
    public void index(Logger logger, IndexWriter writer, String col,
                      String []rowids, boolean withMasterColumn) throws IOException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sqlStr = this.getSelectStmt(col, true, withMasterColumn).replace("L$PT, 10","L$PT, "+rowids.length);
        logger.info(sqlStr);
        try {
            stmt = this.conn.prepareStatement(sqlStr);
            ODCIRidList rids = new ODCIRidList(rowids);
            stmt.setObject(1, rids);
            rs = stmt.executeQuery();
            ResultSetMetaData mtdt = rs.getMetaData();
            int numExtraCols =
                mtdt.getColumnCount() - ((withMasterColumn) ? 2 : 1);
            int offset = (withMasterColumn) ? 3 : 2;
            String[] extraCols = new String[numExtraCols];
            Object[] extraValues = new Object[numExtraCols];
            for (int i = 0; i < numExtraCols; i++)
                extraCols[i] = mtdt.getColumnName(i + offset);
            while (rs.next()) {
                String rowid = rs.getString(1);
                Object value = (withMasterColumn) ? rs.getObject(2) : null;
                for (int i = 0; i < numExtraCols; i++) {
                    extraValues[i] = rs.getObject(i + offset);
                }
                Document doc =
                    userDataStore.getDocument(rowid, col, value,
                                              extraCols, extraValues);
                writer.addDocument(doc);
                if (logger.isLoggable(Level.FINER))
                    logger.fine(doc.toString());
            }
        } catch (SQLException e) {
            logger.throwing("org.apache.lucene.indexer.TableIndexer", "index",
                            e);
            throw new RuntimeException("Error indexing the table: " +
                                       schemaName + "." + tableName + " - " +
                                       e.getMessage() + " query: " + sqlStr);
        } finally {
            OJVMUtil.closeDbResources(stmt, rs);
        }
    }

    /**
     * Index a table executing a full scan in deferred index mode
     * called from LuceneDomainIndex class
     * @throws IOException
     * @see LuceneDomainIndex#ODCIIndexCreate
     */
    public void index(Logger logger, String idxName, int rowBatchCount) {
        PreparedStatement stmt = null;
        CallableStatement cs = null;
        ResultSet rs = null;
        String sqlStr = this.getSelectStmt("", false, false);
        logger.info("index(Logger logger, String idxName, int rowBatchCount), Performing:\n" + sqlStr);
        ArrayList arr = new ArrayList(rowBatchCount);
        int count = 0;
        try {
            cs = this.conn.prepareCall(enqueueInsertChangeStmt);
            cs.setString(1,idxName);
            stmt = this.conn.prepareStatement(sqlStr);
            rs = stmt.executeQuery();
            while (rs.next()) {
                String rowid = rs.getString(1);
                arr.add(rowid);
                if (++count == rowBatchCount) {
                    // enqueue here rowids to insert by AQ Callback
                    ODCIRidList rids = new ODCIRidList((String [])(arr.toArray(new String[1])));
                    logger.info("Enqueuing for insert "+rids.length()+" rowids");
                    cs.setObject(2,rids);
                    cs.executeUpdate();
                    arr.clear();
                    count = 0;
                }
            }
            if (count>0) {
                // enqueue remainded rowids to insert by AQ Callback
                ODCIRidList rids = new ODCIRidList((String [])(arr.toArray(new String[1])));
                logger.info("Enqueuing for insert "+rids.length()+" rowids");
                cs.setObject(2,rids);
                cs.executeUpdate();
            }
        } catch (SQLException e) {
            logger.throwing("org.apache.lucene.indexer.TableIndexer","index",e);
            throw new RuntimeException("Error deferred indexing the table: " +
                                       schemaName + "." + tableName + " - " +
                                       e.getMessage() + " query: " +
                                       sqlStr);
        } finally {
            OJVMUtil.closeDbResources(stmt, rs);
            OJVMUtil.closeDbResources(cs, null);
        }
    }
    
    /**
     * Index a table executing a full scan, called from LuceneDomainIndex class
     * @param writer Lucene writer
     * @param col master column of the index
     * @throws IOException
     * @see LuceneDomainIndex#ODCIIndexCreate
     */
    public void index(Logger logger, IndexWriter writer, String col, boolean withMasterColumn) throws IOException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sqlStr = this.getSelectStmt(col, false, withMasterColumn);
        logger.info("index(Logger logger, IndexWriter writer, String col, boolean withMasterColumn), Performing:\n" + sqlStr);
        try {
            stmt = this.conn.prepareStatement(sqlStr);
            rs = stmt.executeQuery();
            ResultSetMetaData mtdt = rs.getMetaData();
            int numExtraCols = mtdt.getColumnCount()-((withMasterColumn) ? 2 : 1);
            int offset = (withMasterColumn) ? 3 : 2;
            String[] extraCols = new String[numExtraCols];
            for (int i=0;i<numExtraCols;i++)
                extraCols[i] = mtdt.getColumnName(i+offset);
            while (rs.next()) {
                String rowid = rs.getString(1);
                Object value = (withMasterColumn) ? rs.getObject(2) : null;
                Object[] extraValues = new Object[numExtraCols];
                for (int i = 0; i < numExtraCols; i++) {
                    extraValues[i] = rs.getObject(i + offset);
                }
                Document doc =
                    userDataStore.getDocument(rowid, col, value, extraCols,
                                              extraValues);
                writer.addDocument(doc);
                if (logger.isLoggable(Level.FINER))
                    logger.fine(doc.toString());
            }
        } catch (SQLException e) {
            logger.throwing("org.apache.lucene.indexer.TableIndexer","index",e);
            throw new RuntimeException("Error indexing the table: " +
                                       schemaName + "." + tableName + " - " +
                                       e.getMessage() + " query: " +
                                       sqlStr);
        } finally {
            OJVMUtil.closeDbResources(stmt, rs);
        }
    }

    /**
     * setter method for user data store property
     * @param param
     */
    public void setUserDataStore(UserDataStore param) {
        this.userDataStore = param;
    }

    /**
     * getter method for user data store property
     * @return
     */
    public UserDataStore getUserDataStore() {
        return userDataStore;
    }

    /**
     * setter method for extra columns property string
     * @param param
     */
    public void setExtraColsStr(String param) {
        this.extraColsStr = param;
    }

    /**
     * getter method for extra columns property string
     * @return
     */
    public String getExtraColsStr() {
        return extraColsStr;
    }

    /**
     * setter method for extra tabs property
     * @param param
     */
    public void setExtraTabsStr(String param) {
        this.extraTabsStr = param;
    }

    /**
     * getter method for extra tabs property
     * @return
     */
    public String getExtraTabsStr() {
        return extraTabsStr;
    }

    /**
     * setter method for where condition property
     * @param param
     */
    public void setExtraWhereStr(String param) {
        this.extraWhereStr = param;
    }

    /**
     * getter method for where condition property
     * @return
     */
    public String getExtraWhereStr() {
        return extraWhereStr;
    }

    /**
     * setter method for lockMasterTable property
     *
     * @param param
     */
    public void setLockMasterTable(boolean param) {
        this.lockMasterTable = param;
    }

    /**
     * getter method for lockMasterTable property
     *
     * @return
     */
    public boolean getLockMasterTable() {
        return lockMasterTable;
    }
}
