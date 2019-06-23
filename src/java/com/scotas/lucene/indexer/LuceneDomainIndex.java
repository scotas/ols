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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import java.math.BigDecimal;

import java.rmi.Naming;

import java.rmi.RemoteException;


import java.sql.Connection;
import java.sql.SQLException;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.ODCI.ODCIEnv;
import oracle.ODCI.ODCIIndexCtx;
import oracle.ODCI.ODCIIndexInfo;
import oracle.ODCI.ODCIPartInfo;
import oracle.ODCI.ODCIPredInfo;
import oracle.ODCI.ODCIQueryInfo;
import oracle.ODCI.ODCIRidList;

import oracle.aurora.vm.OracleRuntime;

import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleTypes;
import oracle.jdbc.driver.OracleConnection;

import oracle.jpub.runtime.MutableStruct;

import oracle.sql.CLOB;
import oracle.sql.CustomDatum;
import oracle.sql.CustomDatumFactory;
import oracle.sql.Datum;
import oracle.sql.STRUCT;

import oracle.xdb.XMLType;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.LogDocMergePolicy;
import org.apache.lucene.index.LogMergePolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import com.scotas.lucene.search.CountHitCollector;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.store.LockObtainFailedException;
import com.scotas.lucene.store.OJVMDirectory;
import com.scotas.lucene.store.OJVMUtil;
import com.scotas.lucene.util.StringUtils;

import java.util.Map;
import java.util.Random;

import oracle.ODCI.AnyData;
import oracle.ODCI.ODCIColInfo;
import oracle.ODCI.ODCICompQueryInfo;
import oracle.ODCI.ODCIFilterInfo;
import oracle.ODCI.ODCIFilterInfoList;
import oracle.ODCI.ODCIOrderByInfo;
import oracle.ODCI.ODCIOrderByInfoList;

import org.apache.lucene.util.Version;

/**
 * A Data Catridge API implementation.
 *
 */
public class LuceneDomainIndex implements CustomDatum, CustomDatumFactory {

    /**
     * Java Util Logging variables and default values
     */
    private static Logger logger = null;

    private static IndexScan localSearcher = new IndexScanImpl();
    
    private static IndexUpdate localUpdater = new IndexUpdateImpl(true);

    private static Hashtable remoteSearchers = new Hashtable();

    private static Hashtable remoteUpdaters = new Hashtable();

    /**
     * Constant used to get Logger name
     */
    static final String CLASS_NAME = LuceneDomainIndex.class.getName();

    static {
        logger = Logger.getLogger(CLASS_NAME);
        // default Log level, override it using
        // LuceneDomainIndex.setLogLevel('level')
        logger.setLevel(Level.WARNING);
    }

    static final String enqueueDeleteChangeStmt = 
        "call LuceneDomainIndex.enqueueChange(?,?,'delete')";

    static final String enableCallBackStmt =
        "call LuceneDomainIndex.enableCallBack(?)";

    static final String disableCallBackStmt =
        "call LuceneDomainIndex.disableCallBack(?)";

    static final String crtLuceneTableStmt =
        "call LuceneDomainIndex.createTable(?,?)";

    static final String drpLuceneTableStmt =
        "call LuceneDomainIndex.dropTable(?)";

    static final String crtLuceneQueueStmt =
        "call LuceneDomainAdm.createQueue(?)";

    static final String crtLuceneSeqStmt =
        "create sequence %IDX%$S cycle nocache minvalue 0 maxvalue ";

    static final String lckLuceneTableStmt =
        "lock table %IDX%$T in exclusive mode";

    static final String drpLuceneQueueStmt =
        "call LuceneDomainAdm.dropQueue(?)";

    static final String purgueLuceneQueueStmt =
        "call LuceneDomainAdm.purgueQueue(?)";

    static final String drpLuceneSeqStmt = "drop sequence %IDX%$S";

    public static final String _SQL_NAME = "LUCENE.LUCENEDOMAININDEX";

    public static final int _SQL_TYPECODE = OracleTypes.STRUCT;

    static final java.math.BigDecimal SUCCESS = new java.math.BigDecimal("0");

    static final java.math.BigDecimal ERROR = new java.math.BigDecimal("1");

    static final int TRUE = 1;

    static final int FALSE = 0;

    static int[] _sqlType = { 4 };

    /**
     * from ODCICONST.AlterIndexNone
     */
    static final BigDecimal ODCIConstAlterIndexNone = new BigDecimal(0);

    /**
     * from ODCICONST.AlterIndexRename
     */
    static final BigDecimal ODCIConstAlterIndexRename = new BigDecimal(1);

    /**
     * from ODCICONST.AlterIndexRebuild
     */
    static final BigDecimal ODCIConstAlterIndexRebuild = new BigDecimal(2);

    /**
     * from ODCICONST.AlterIndexRebuildOnline
     */
    static final BigDecimal ODCIConstAlterIndexRebuildOnLine =
        new BigDecimal(3);

    /**
     * from ODCIConst.FirstCall
     */
    static final BigDecimal ODCIConstFirstCall =
      new BigDecimal(1);

    /**
     * from ODCIConst.IntermediateCall
     */
    static final BigDecimal ODCIConstIntermediateCall =
      new BigDecimal(2);

    /**
     * from ODCIConst.FinalCall
     */
    static final BigDecimal ODCIConstFinalCall =
      new BigDecimal(3);

    static final String defaultLobParameters =
        "PCTVERSION 0 DISABLE STORAGE IN ROW CACHE READS NOLOGGING";

    /**
     * SYS.ODCIConst.QueryFirstRows
     */
    static final int QUERY_FIRST_ROWS = 1;

    /**
     * SYS.ODCIConst.QueryAllRows
     */
    static final int QUERY_ALL_ROWS = 2;

    /**
     * SYS.ODCIConst.QuerySortAsc
     */
    static final int QUERY_SORT_ASC = 4;

    /**
     * SYS.ODCIConst.QuerySortDesc
     */
    static final int QUERY_SORT_DESC = 8;

    /**
     * SYS.ODCIConst.QueryBlocking
     */
    static final int QUERY_BLOCKING = 16;

    /**
     * SYS.ODCIPredInfo.Flags 
     */
    static final int PredExactMatch   =   1;
    static final int PredPrefixMatch  =   2;
    static final int PredIncludeStart =   4;
    static final int PredIncludeStop  =   8;
    static final int PredObjectFunc   =  16;
    static final int PredObjectPkg    =  32;
    static final int PredObjectType   =  64;
    static final int PredMultiTable   = 128;
    static final int PredNotEqual     = 256;

    public static final Version LUCENE_COMPAT_VERSION = Version.LUCENE_40;

    public static final String DEFAULT_ANALYZER = "org.apache.lucene.analysis.core.SimpleAnalyzer";

    private static final Random RANDOM = new Random();

    static CustomDatumFactory[] _factory = new CustomDatumFactory[1];

    MutableStruct _struct;

    static final LuceneDomainIndex _LuceneDomainIndexFactory =
        new LuceneDomainIndex();

    public static CustomDatumFactory getFactory() {
        return _LuceneDomainIndexFactory;
    }

    /* constructor */

    public LuceneDomainIndex() {
        _struct = new MutableStruct(new Object[1], _sqlType, _factory);
    }

    /* CustomDatum interface */

    public Datum toDatum(OracleConnection c) throws SQLException {
        return _struct.toDatum((Connection)c, _SQL_NAME);
    }

    /* CustomDatumFactory interface */

    public CustomDatum create(Datum d, int sqlType) throws SQLException {
        if (d == null)
            return null;
        LuceneDomainIndex o = new LuceneDomainIndex();
        o._struct = new MutableStruct((STRUCT)d, _sqlType, _factory);
        return o;
    }

    /* shallow copy method: give object same attributes as argument */

    void shallowCopy(LuceneDomainIndex d) throws SQLException {
        _struct = d._struct;
    }

    /* accessor methods */

    public Integer getScanctx() throws SQLException {
        return (Integer)_struct.getAttribute(0);
    }

    public void setScanctx(Integer scanctx) throws SQLException {
        _struct.setAttribute(0, scanctx);
    }

    /**
     * @param ia
     * @return an string with the indexschema.indexname[.partitionname] used as
     *         index indentifier by Lucene Domain Index
     * @throws SQLException
     */
    public static String getIndexPrefix(ODCIIndexInfo ia) throws SQLException {
        String indexPrefix;
        if (ia.getIndexPartition() != null)
            indexPrefix =
                    ia.getIndexSchema() + "." + ia.getIndexName() + "#" + ia.getIndexPartition();
        else
            indexPrefix = ia.getIndexSchema() + "." + ia.getIndexName();
        return indexPrefix;
    }

    /**
     * Invoked when a domain index or a domain index partition is altered using
     * an ALTERINDEX or an ALTER INDEX PARTITION statement.
     *
     * @param ia
     *            Contains information about the index and the indexed column
     * @param parms
     *            Parameter string, IN: With ALTER INDEX PARAMETERS or ALTER
     *            INDEX REBUILD, contains the user specified parameter string
     *            With ALTER INDEX RENAME, contains the new name of the domain
     *            index OUT: Valid only with ALTER INDEX PARAMETERS or ALTER
     *            INDEX REBUILD; Contains the resultant string to be stored in
     *            system catalogs
     * @param option
     *            Specifies one of the following options: - AlterIndexNone if
     *            ALTER INDEX [PARTITION] PARAMETERS - AlterIndexRename if ALTER
     *            INDEX RENAME [PARTITION] - AlterIndexRebuild if ALTER INDEX
     *            REBUILD [PARTITION] [PARALLEL (DEGREE deg)] [PARAMETERS] -
     *            AlterIndexUpdBlockRefs if ALTER INDEX [schema.]index UPDATE
     *            BLOCK REFERENCES
     * @param env
     *            The environment handle passed to the routine
     * @return ODCIConst.Success on success, ODCIConst.Error on error, or
     *         ODCIConst.Warning otherwise.
     * @throws SQLException
     */
    public static java.math.BigDecimal ODCIIndexAlter(ODCIIndexInfo ia,
                                                      String[] parms,
                                                      java.math.BigDecimal option,
                                                      ODCIEnv env) throws SQLException,
                                                                          IOException {
        logger.entering(CLASS_NAME, "ODCIIndexAlter",
                        new Object[] { ia, parms, option, env });
        OJVMUtil.dumpIA(logger, ia);
        OJVMUtil.dumpEnv(logger, env);
        String directoryPrefix = getIndexPrefix(ia);
        Parameters parameters = Parameters.getParameters(directoryPrefix);
        OracleCallableStatement cs = null;
        Connection conn = OJVMUtil.getConnection();
        try {
            String logLevel = parameters.getParameter("LogLevel", "WARNING");
            try {
                logger.setLevel(Level.parse(logLevel)); // Restore log level
            } catch (IllegalArgumentException e) {
                logger.setLevel(Level.WARNING);
                logger.warning(e.getMessage());
            }
            String updaterHost = parameters.getLuceneUpdater();
            if (!"local".equalsIgnoreCase(updaterHost)) {
              IndexUpdate remoteUpdater = getUpdater(updaterHost);
              remoteUpdater.forceClose(directoryPrefix);
            }
            String columnName =
                ia.getIndexCols().getElement(0).getColName().replaceAll("\"",
                                                                        "");
            String columnTypeName =
                ia.getIndexCols().getElement(0).getColTypeName();
            String tableSchema =
                ia.getIndexCols().getElement(0).getTableSchema();
            String tableName = ia.getIndexCols().getElement(0).getTableName();
            String partition =
                ((ia.getIndexPartition() != null && env.getCallProperty() !=
                  null) ? ia.getIndexCols().getElement(0).getTablePartition() :
                 null);
            if (option.equals(ODCIConstAlterIndexNone) ||
                 option.equals(ODCIConstAlterIndexRebuild) ||
                 option.equals(ODCIConstAlterIndexRebuildOnLine)) {
                String[] parmList;
                if (parms != null && parms[0] != null) {
                  String paramstr = parms[0];
                  parmList = paramstr.split(";");
                } else
                  parmList = new String[0];
                for (int i = 0; i < parmList.length; i++) {
                    String[] nameValue = parmList[i].split(":",2);
                    String name = nameValue[0];
                    String value = nameValue[1];
                    int parDegree =
                        Integer.parseInt(parameters.getParameter("ParallelDegree",
                                                                 "0"));
                    boolean indexOnRam =
                        "true".equalsIgnoreCase(parameters.getParameter("IndexOnRam",
                                                                        "true"));
                    if (name.startsWith("~")) { // If parameter name start with
                        // ~ means remove it
                        name = name.substring(1);
                        if (!Parameters.isValidParameterName(name)) {
                            logger.warning("Not validad parameter name: " +
                                           name);
                            logger.exiting(CLASS_NAME, "ODCIIndexAlter",
                                           ERROR);
                            return ERROR;
                        }
                        if ("SyncMode".equalsIgnoreCase(name)) {
                            disableOnLineSync(directoryPrefix, parDegree,
                                              indexOnRam, true);
                            for (int j = 0; j < parDegree; j++) {
                                disableOnLineSync(directoryPrefix + "$" + j,
                                                  parDegree, indexOnRam,
                                                  false);
                            }
                        }
                        if ("ParallelDegree".equalsIgnoreCase(name)) {
                            // drop parallel storage
                            for (int j = 0; j < parDegree; j++) {
                                OJVMDirectory.invalidateCachedEntry(directoryPrefix +
                                                                     "$" + j);
                                dropLuceneStore(directoryPrefix + "$" + j);
                            }
                        }
                        if ("LogLevel".equalsIgnoreCase(name))
                            logger.setLevel(Level.WARNING); // default level
                        parameters.removeParameter(name);
                        logger.config("remove parameter : '" + name + "'");
                    } else {
                        if (!Parameters.isValidParameterName(name)) {
                            logger.warning("Not validad parameter name: " +
                                           nameValue[0]);
                            logger.exiting(CLASS_NAME, "ODCIIndexAlter",
                                           ERROR);
                            return ERROR;
                        }
                        parameters.setParameter(name, value);
                        logger.config("store parameter : '" + name +
                                      "' with value '" + value + "'");
                        if ("SyncMode".equalsIgnoreCase(name))
                            if ("OnLine".equalsIgnoreCase(value)) {
                                enableOnLineSync(directoryPrefix, parDegree,
                                                 indexOnRam, true);
                                for (int j = 0; j < parDegree; j++) {
                                    enableOnLineSync(directoryPrefix + "$" + j,
                                                     parDegree, indexOnRam,
                                                     false);
                                }
                            } else {
                                disableOnLineSync(directoryPrefix, parDegree,
                                                  indexOnRam, true);
                                for (int j = 0; j < parDegree; j++) {
                                    disableOnLineSync(directoryPrefix + "$" +
                                                      j, parDegree, indexOnRam,
                                                      false);
                                }
                            }
                        if ("ParallelDegree".equalsIgnoreCase(name)) {
                            // re-create parallel storage according to new value
                            for (int j = 0; j < parDegree; j++) {
                                OJVMDirectory.invalidateCachedEntry(directoryPrefix +
                                                                     "$" + j);
                                dropLuceneStore(directoryPrefix + "$" + j);
                            }
                            String LobStorageParameters =
                                parameters.getParameter("LobStorageParameters",
                                                        defaultLobParameters);
                            boolean onLine =
                                "OnLine".equalsIgnoreCase(parameters.getParameter("SyncMode",
                                                                                  "Deferred"));
                            if (onLine) // disable callback which is associated
                                // to a different parDegree
                                disableOnLineSync(directoryPrefix, parDegree,
                                                  indexOnRam, true);
                            // re-create slave tables using new value of
                            // parDegree
                            parDegree = Integer.parseInt(value);
                            for (int j = 0; j < parDegree; j++) {
                                createLuceneStore(directoryPrefix + "$" + j,
                                                  LobStorageParameters);
                                OJVMDirectory dir = OJVMDirectory.getDirectory(directoryPrefix +
                                                               "$" + j);
                                dir.saveParameters(parameters);
                                IndexWriter writer =
                                    getIndexWriterForDir(dir, true);
                                writer.commit();
                                writer.waitForMerges();
                                writer.close();
                                dir.close();
                                if (onLine)
                                    enableOnLineSync(directoryPrefix + "$" + j,
                                                     parDegree, indexOnRam,
                                                     false);
                            }
                            if (onLine) // re-enable callback on master table
                                enableOnLineSync(directoryPrefix, parDegree,
                                                 indexOnRam, true);
                        }
                        if ("LogLevel".equalsIgnoreCase(name))
                            logger.setLevel(Level.parse(value));
                    }
                }
            } else {
                logger.severe("ODCIIndexAlter error parameter is null or option not implemented. Option: " +
                              option);
                throw new SQLException("ODCIIndexAlter error parameter is null or option not implemented. Option: " +
                                       option);
            }
            parameters.setParameter("ColName", columnName);
            parameters.setParameter("TypeName", columnTypeName);
            parameters.setParameter("TableSchema", tableSchema);
            parameters.setParameter("TableName", tableName);
            parameters.setParameter("Partition", partition);
            String dfltColumn = parameters.getParameter("DefaultColumn");
            if (dfltColumn == null) // if no user set DefaultColumn, use index
                // Column Name
                parameters.setParameter("DefaultColumn", columnName);
            parameters.save(directoryPrefix);
            if (option.equals(ODCIConstAlterIndexRebuild) ||
                option.equals(ODCIConstAlterIndexRebuildOnLine)) {
                try {
                    cs = (OracleCallableStatement)conn.prepareCall(StringUtils.replace(
                          lckLuceneTableStmt, "%IDX%", directoryPrefix));
                    cs.execute();
                    rebuild(directoryPrefix);
                    conn.commit(); // release lock
                } catch (SQLException e) {
                    logger.severe("failed to rebuild index: " +
                                  e.getMessage());
                    logger.throwing(CLASS_NAME, "ODCIIndexAlter", e);
                    logger.exiting(CLASS_NAME, "ODCIIndexAlter", ERROR);
                    return ERROR;
                } finally {
                    OJVMUtil.closeDbResources(cs, null);
                }
            }
            String searcherHost = parameters.getLuceneRandomSearcher();
            IndexScan indexScan = getSearcher(searcherHost);
            indexScan.refreshCache(directoryPrefix);
            logger.config("Parameters:\n" +
                    parameters.toString());
            // return only internals paramaters to be stored in SYSTEM's views
            parms[0] = parameters.toString();
        } catch (SQLException e) {
            logger.severe("failed to alter index: " + e.getMessage());
            logger.throwing(CLASS_NAME, "ODCIIndexAlter", e);
            return ERROR;
        } catch (RemoteException e) {
            logger.severe("failed to refresh slave cache: " + e.getMessage());
            logger.throwing(CLASS_NAME, "ODCIIndexAlter", e);
            return ERROR;
        }
        logger.exiting(CLASS_NAME, "ODCIIndexAlter", SUCCESS);
        return SUCCESS;
    }

    /**
     * When a user issues a CREATE INDEX statement that references the
     * indextype, Oracle calls your ODCIIndexCreate() method, passing it any
     * parameters specified as part of the CREATE INDEX... PARAMETERS (...)
     * statement, plus the description of the index. Typically, this method
     * creates the tables or files in which you plan to store index data. Unless
     * the base table is empty, the method should also build the index.
     *
     * @param ia
     *            Contains information about the index and the indexed column
     * @param parms
     *            Parameter string, IN: With ALTER INDEX PARAMETERS or ALTER
     *            INDEX REBUILD, contains the user specified parameter string
     *            With ALTER INDEX RENAME, contains the new name of the domain
     *            index OUT: Valid only with ALTER INDEX PARAMETERS or ALTER
     *            INDEX REBUILD; Contains the resultant string to be stored in
     *            system catalogs
     * @param env
     *            The environment handle passed to the routine
     * @return ODCIConst.Success on success, ODCIConst.Error on error, or
     *         ODCIConst.Warning otherwise.
     * @throws SQLException
     */
    public static java.math.BigDecimal ODCIIndexCreate(ODCIIndexInfo ia,
                                                       String parms,
                                                       ODCIEnv env) throws SQLException {
        OJVMDirectory dir = null;
        IndexWriter writer = null;
        Parameters parameters = new Parameters();
        logger.entering(CLASS_NAME, "ODCIIndexCreate",
                        new Object[] { ia, parms, env });
        OJVMUtil.dumpIA(logger, ia);
        OJVMUtil.dumpEnv(logger, env);
        if (env != null && env.getCallProperty() != null) 
          // Partitioning suppport ignore first and last call iteration
          if (env.getCallProperty().equals(ODCIConstFirstCall) 
              || env.getCallProperty().equals(ODCIConstFinalCall))
            return SUCCESS;
        OracleCallableStatement cs = null;
        Connection conn;
        try {
            String directoryPrefix = getIndexPrefix(ia);
            if (parms != null) {
                String[] parmList = parms.split(";");
                for (int i = 0; i < parmList.length; i++) {
                    String[] nameValue = parmList[i].split(":",2);
                    String name = nameValue[0];
                    if (name.startsWith("~"))
                        continue;
                    if (!Parameters.isValidParameterName(nameValue[0])) {
                        logger.warning("Not validad parameter name: " + name);
                        logger.exiting(CLASS_NAME, "ODCIIndexCreate", ERROR);
                        return ERROR;
                    }
                    if (name.equalsIgnoreCase("LogLevel"))
                        logger.setLevel(Level.parse(nameValue[1]));
                    parameters.setParameter(name, nameValue[1]);
                    logger.config("store parameter : '" + nameValue[0] +
                                  "' value '" + nameValue[1] + "'");
                }
            }
            String columnName =
                ia.getIndexCols().getElement(0).getColName().replaceAll("\"",
                                                                        "");
            String columnTypeName =
                ia.getIndexCols().getElement(0).getColTypeName();
            String tableSchema =
                ia.getIndexCols().getElement(0).getTableSchema();
            String tableName = ia.getIndexCols().getElement(0).getTableName();
            String partition =
                ((ia.getIndexPartition() != null && env.getCallProperty() !=
                  null) ? ia.getIndexCols().getElement(0).getTablePartition() :
                 null);
            parameters.setParameter("ColName", columnName);
            parameters.setParameter("TypeName", columnTypeName);
            parameters.setParameter("TableSchema", tableSchema);
            parameters.setParameter("TableName", tableName);
            parameters.setParameter("Partition", partition);
            String syncMode = parameters.getParameter("SyncMode", "Deferred");
            boolean populateIndex =
                "true".equalsIgnoreCase(parameters.getParameter("PopulateIndex",
                                                                "true"));
            String LobStorageParameters =
                parameters.getParameter("LobStorageParameters",
                                        defaultLobParameters);
            logger.info("Indexing column: '" + columnName + "'");
            logger.info("Internal Parameters ("+directoryPrefix+"):\n" +
                    parameters.toString());
            int parDegree =
                Integer.parseInt(parameters.getParameter("ParallelDegree",
                                                         "0"));
            int batchCount =
                Integer.parseInt(parameters.getParameter("BatchCount", "115"));
            boolean indexOnRam =
                "true".equalsIgnoreCase(parameters.getParameter("IndexOnRam",
                                                                "true"));
            String dfltColumn = parameters.getParameter("DefaultColumn");
            if (dfltColumn == null) // if no user set DefaultColumn, use index
                // Column Name
                parameters.setParameter("DefaultColumn", columnName);
            if (ia.getIndexCols().length()>1) { // filter by, order by pred.
                String extraCols = parameters.getParameter("ExtraCols","");
                extraCols = getExtraColsFromIA(ia,extraCols);
                // store computed extra cols
                parameters.setParameter("ExtraCols", extraCols);
            }
            createLuceneStore(directoryPrefix, LobStorageParameters);
            for (int i = 0; i < parDegree; i++) {
                createLuceneStore(directoryPrefix + "$" + i,
                                  LobStorageParameters);
                dir = OJVMDirectory.getDirectory(directoryPrefix + "$" + i);
                dir.saveParameters(parameters);
                writer = getIndexWriterForDir(dir, true);
                writer.commit();
                writer.waitForMerges();
                writer.close();
                dir.close();
            }
            dir = OJVMDirectory.getDirectory(directoryPrefix);
            dir.saveParameters(parameters);
            conn = dir.getConnection();
            cs = (OracleCallableStatement)conn.prepareCall(StringUtils.replace(lckLuceneTableStmt, "%IDX%", directoryPrefix));
            cs.execute();
            cs.close();
            cs = null;
            TableIndexer index =
                new TableIndexer(dir.getConnection(), tableSchema, tableName,
                                 partition);
            String extraTabs = parameters.getParameter("ExtraTabs");
            String whereCondition = parameters.getParameter("WhereCondition");
            boolean lockMasterTable =
                "true".equalsIgnoreCase(parameters.getParameter("LockMasterTable", "true"));
            logger.info(" ExtraTabs: " + extraTabs + " WhereCondition: " +
                        whereCondition + " LockMasterTable: " +
                        lockMasterTable);
            index.setExtraTabsStr(extraTabs);
            index.setExtraWhereStr(whereCondition);
            writer = getIndexWriterForDir(dir, true);
            index.setLockMasterTable(lockMasterTable);
            if ("OnLine".equalsIgnoreCase(syncMode)) { // Finally enable AQ
                // Callback
                enableOnLineSync(directoryPrefix, parDegree, indexOnRam, true);
                for (int i = 0; i < parDegree; i++) {
                    enableOnLineSync(directoryPrefix + "$" + i, parDegree,
                                     indexOnRam, false);
                }
            }
            if (populateIndex && "OnLine".equalsIgnoreCase(syncMode)) {
                // User Data Store is not relevant here, only enqueue all rowids
                // returned
                // By a full scan of the table taking into account ExtraTabs and
                // WhereCondition
                logger.info("BatchCount: " + batchCount);
                index.setExtraColsStr("null");
                index.index(logger, directoryPrefix, batchCount);
            } else if (populateIndex) {
                // Create the index and fill it using TableIndexer.index()
                // method
                UserDataStore userDataStore = getUserDataStore(parameters);
                boolean includeMasterColumn =
                    "true".equalsIgnoreCase(parameters.getParameter("IncludeMasterColumn",
                                                                    "true"));
                String extraCols = parameters.getParameter("ExtraCols");
                logger.info("ExtraCols: " + extraCols + " User Data Store: " +
                            userDataStore);
                index.setUserDataStore(userDataStore);
                index.setExtraColsStr(extraCols);
                index.index(logger, writer, columnName, includeMasterColumn);
                dir.purge();
            }
            writer.commit();
            writer.waitForMerges();
            dir.removeCachedSearcher();
            conn.commit();
        } catch (SQLException e) {
            logger.severe("failed to create index: " + e.getMessage());
            logger.throwing(CLASS_NAME, "ODCIIndexCreate", e);
            return ERROR;
        } catch (IOException e) {
            logger.severe("failed to create index: " + e.getMessage());
            logger.throwing(CLASS_NAME, "ODCIIndexCreate", e);
            return ERROR;
        } finally {
            try {
                if (writer != null)
                    writer.close();
                writer = null;
                if (dir != null)
                    dir.close();
                dir = null;
            } catch (IOException e) {
                logger.severe("failed to create index: " + e.getMessage());
                logger.throwing(CLASS_NAME, "ODCIIndexCreate", e);
                return ERROR;
            }
        }
        logger.exiting(CLASS_NAME, "ODCIIndexCreate", SUCCESS);
        return SUCCESS;
    }

    /**
     * When a user issues a DROP statement against a table that contains a
     * column or object type attribute indexed by your indextype, Oracle calls
     * your ODCIIndexDrop() method. This method should leave the domain index
     * empty.
     *
     * @param ia
     * @param env
     * @return
     * @throws SQLException
     */
    public static java.math.BigDecimal ODCIIndexDrop(ODCIIndexInfo ia,
                                                     ODCIEnv env) throws SQLException {
        logger.entering(CLASS_NAME, "ODCIIndexDrop", new Object[] { ia, env });
        OJVMUtil.dumpIA(logger, ia);
        OJVMUtil.dumpEnv(logger, env);
        if (env != null && env.getCallProperty() != null) 
          // Partitioning suppport ignore first and last call iteration
          if (env.getCallProperty().equals(ODCIConstFirstCall) 
              || env.getCallProperty().equals(ODCIConstFinalCall))
            return SUCCESS;
        String directoryPrefix = getIndexPrefix(ia);
        Parameters parameters = Parameters.getParameters(directoryPrefix);
        int parDegree =
            Integer.parseInt(parameters.getParameter("ParallelDegree", "0"));
        boolean indexOnRam =
            "true".equalsIgnoreCase(parameters.getParameter("IndexOnRam",
                                                            "true"));
        try {
            String logLevel = parameters.getParameter("LogLevel", "WARNING");
            logger.setLevel(Level.parse(logLevel)); // Restore log level
            if ("OnLine".equalsIgnoreCase(parameters.getParameter("SyncMode"))) {
                disableOnLineSync(directoryPrefix, parDegree, indexOnRam,
                                  true);
                for (int i = 0; i < parDegree; i++) {
                    disableOnLineSync(directoryPrefix + "$" + i, parDegree,
                                      indexOnRam, false);
                }
            }
            // remove pending messages
            discard(directoryPrefix);
            // remove this directory from cache
            OJVMDirectory.invalidateCachedEntry(directoryPrefix);
        } catch (IOException e) {
            logger.severe("failed to drop index: " + e.getMessage());
        } catch (InstantiationError e) {
            logger.severe("failed to drop index: " + e.getMessage());
        }
        dropLuceneStore(directoryPrefix);
        for (int i = 0; i < parDegree; i++) {
            try {
                OJVMDirectory.invalidateCachedEntry(directoryPrefix+ "$" + i);
            } catch (IOException e) {
                logger.severe("failed to drop slave index: " + e.getMessage());
            }
            dropLuceneStore(directoryPrefix + "$" + i);
        }
        try {
            String updaterHost = parameters.getLuceneUpdater();
            if (!"local".equalsIgnoreCase(updaterHost)) {
              IndexUpdate remoteUpdater = getUpdater(updaterHost);
              remoteUpdater.forceClose(directoryPrefix);
            }
            String searcherHost = parameters.getLuceneRandomSearcher();
            IndexScan indexScan = getSearcher(searcherHost);
            indexScan.refreshCache(directoryPrefix);
        } catch (RemoteException e) {
            logger.severe("failed to refresh slave cache: " + e.getMessage());
        }
        logger.exiting(CLASS_NAME, "ODCIIndexDrop", SUCCESS);
        return SUCCESS;
    }

    /**
     * Invoked during partition maintenance operations. Patches the indextype 
     * metadata tables to correctly reflect the partition maintenance operation.
     * @param ia
     * @param pinfo
     * @param env
     * @return
     * @throws SQLException
     */
    public static java.math.BigDecimal ODCIIndexUpdPartMetadata(ODCIIndexInfo ia,
                ODCIPartInfo pinfo,
		            ODCIEnv env) throws SQLException {
      logger.entering(CLASS_NAME, "ODCIIndexUpdPartMetadata",
                      new Object[] { ia, pinfo, env });
        OJVMUtil.dumpIA(logger, ia);
        OJVMUtil.dumpPartInfo(logger, pinfo);
        OJVMUtil.dumpEnv(logger, env);
      logger.exiting(CLASS_NAME, "ODCIIndexUpdPartMetadata", SUCCESS);
      return SUCCESS;
    }
  
    /**
     * This method is invoked when an 
     * ALTER TABLE EXCHANGE PARTITION...INCLUDING INDEXES command is issued on
     * a partitioned table that has a defined local domain index.
     * @param ia
     * @param ia1
     * @param env
     * @return
     * @throws SQLException
     */
    public static java.math.BigDecimal ODCIIndexExchangePartition(ODCIIndexInfo ia,
              ODCIIndexInfo ia1,
              ODCIEnv env) throws SQLException {
      logger.entering(CLASS_NAME, "ODCIIndexExchangePartition",
                    new Object[] { ia, ia1, env });
        OJVMUtil.dumpIA(logger, ia);
        OJVMUtil.dumpIA(logger, ia1);
        OJVMUtil.dumpEnv(logger, env);
      logger.exiting(CLASS_NAME, "ODCIIndexExchangePartition", SUCCESS);
      return SUCCESS;
    }
  
    /**
     * Invoked when a ALTER TABLE MERGE PARTITION is issued on range partitioned
     * table on which a domain index is defined.
     * @param ia
     * @param p1
     * @param p2
     * @param parms
     * @param env
     * @return
     * @throws SQLException
     */
    public static java.math.BigDecimal ODCIIndexMergePartition(ODCIIndexInfo ia,
            ODCIPartInfo p1,
            ODCIPartInfo p2,
            String parms,
            ODCIEnv env) throws SQLException {
      logger.entering(CLASS_NAME, "ODCIIndexMergePartition",
                      new Object[] { ia, p1, p2, parms, env });
        OJVMUtil.dumpIA(logger, ia);
        OJVMUtil.dumpEnv(logger, env);
      BigDecimal retVal;
      Parameters pars = Parameters.getParameters(getIndexPrefix(ia));
      // creating new partitions leave it unusable, user should call them rebuild
      pars.setParameter("PopulateIndex", "false");
      String userParameters = pars.getUserParameters();
      logger.info("Partition parameters: " + userParameters);
      // Drop first initial partition
      logger.info("Dropping index partition: " + 
                  ia.getIndexPartition() + " on table partition: " +
                  ia.getIndexCols().getElement(0).getTablePartition());
      retVal = ODCIIndexDrop(ia, env);
      if (ERROR.equals(retVal))
          return retVal;
      // Drop second partition
        OJVMUtil.dumpPartInfo(logger, p1);
      ia.setIndexPartition(p1.getIndexPartition());
      ia.getIndexCols().getElement(0).setTablePartition(p1.getTablePartition());
      logger.info("Dropping index partition: " + 
                  ia.getIndexPartition() + " on table partition: " +
                  ia.getIndexCols().getElement(0).getTablePartition());
      retVal = ODCIIndexDrop(ia, env);
      if (ERROR.equals(retVal))
          return retVal;
      // Create new partition
        OJVMUtil.dumpPartInfo(logger, p2);
      ia.setIndexPartition(p2.getIndexPartition());
      ia.getIndexCols().getElement(0).setTablePartition(p2.getTablePartition());
      logger.info("Creating new merged index partition: " + 
                  ia.getIndexPartition() + " on table partition: " +
                  ia.getIndexCols().getElement(0).getTablePartition());
      retVal = ODCIIndexCreate(ia, userParameters, env);
      logger.exiting(CLASS_NAME, "ODCIIndexMergePartition", retVal);
      return retVal;
    }
  
    /**
     * Invoked when an ALTER TABLE SPLIT PARTITION is invoked on a partitioned 
     * table where a domain index is defined.
     * @param ia
     * @param p1
     * @param p2
     * @param parms
     * @param env
     * @return
     * @throws SQLException
     */
    public static java.math.BigDecimal ODCIIndexSplitPartition(ODCIIndexInfo ia,
            ODCIPartInfo p1,
            ODCIPartInfo p2,
            String parms,
            ODCIEnv env) throws SQLException {
      logger.entering(CLASS_NAME, "ODCIIndexSplitPartition",
                      new Object[] { ia, p1, p2, parms, env });
        OJVMUtil.dumpIA(logger, ia);
        OJVMUtil.dumpEnv(logger, env);
      BigDecimal retVal;
      Parameters pars = Parameters.getParameters(getIndexPrefix(ia));
      // creating new partitions leave it unusable, user should call them rebuild
      pars.setParameter("PopulateIndex", "false");
      String userParameters = pars.getUserParameters();
      logger.info("Partition parameters: " + userParameters);
      // Drop initial partition
      retVal = ODCIIndexDrop(ia, env);
      if (ERROR.equals(retVal))
          return retVal;
      // Create first partition
        OJVMUtil.dumpPartInfo(logger, p1);
      ia.setIndexPartition(p1.getIndexPartition());
      ia.getIndexCols().getElement(0).setTablePartition(p1.getTablePartition());
      logger.info("Creating index partition: " + 
                  ia.getIndexPartition() + " on table partition: " +
                  ia.getIndexCols().getElement(0).getTablePartition());
      retVal = ODCIIndexCreate(ia, userParameters, env);
      if (ERROR.equals(retVal))
          return retVal;
      // Create second partition
        OJVMUtil.dumpPartInfo(logger, p2);
      ia.setIndexPartition(p2.getIndexPartition());
      ia.getIndexCols().getElement(0).setTablePartition(p2.getTablePartition());
      logger.info("Creating index partition: " + 
                  ia.getIndexPartition() + " on table partition: " +
                  ia.getIndexCols().getElement(0).getTablePartition());
      retVal = ODCIIndexCreate(ia, userParameters, env);
      logger.exiting(CLASS_NAME, "ODCIIndexSplitPartition", retVal);
      return retVal;
    }
  
    /**
     * When a user issues a TRUNCATE statement against a table that contains a
     * column or object type attribute indexed by your indextype, Oracle calls
     * your ODCIIndexTruncate() method. This method should leave the domain
     * index empty.
     *
     * @param ia
     * @param env
     * @return
     * @throws SQLException
     */
    public static java.math.BigDecimal ODCIIndexTruncate(ODCIIndexInfo ia,
                                                         ODCIEnv env) throws SQLException {
        OJVMDirectory dir = null;
        String directoryPrefix = getIndexPrefix(ia);
        logger.entering(CLASS_NAME, "ODCIIndexTruncate",
                        new Object[] { ia, env });
        OJVMUtil.dumpIA(logger, ia);
        OJVMUtil.dumpEnv(logger, env);
        if (env != null && env.getCallProperty() != null) 
          // Partitioning suppport ignore first and last call iteration
          if (env.getCallProperty().equals(ODCIConstFirstCall) 
              || env.getCallProperty().equals(ODCIConstFinalCall))
            return SUCCESS;
        try {
            dir = OJVMDirectory.getDirectory(directoryPrefix);
            // Discard pending changes and purge deleted documents
            discard(directoryPrefix);
            String updaterHost = dir.getParameters().getLuceneUpdater();
            if (!"local".equalsIgnoreCase(updaterHost)) {
              IndexUpdate remoteUpdater = getUpdater(updaterHost);
              remoteUpdater.forceClose(directoryPrefix);
            }
            // open a writer object with last argument to true cause truncation
            // on lucene index
            IndexWriter writer =
                getIndexWriterForDir(dir, true);
            writer.close();
            writer = null;
        } catch (IOException e) {
            logger.severe("failed to truncate index: " + e.getMessage());
            logger.throwing(CLASS_NAME, "ODCIIndexTruncate", e);
            return ERROR;
        } finally {
            try {
                if (dir != null)
                    dir.close();
                dir = null;
            } catch (IOException e) {
                logger.severe("failed to invalidate dir cache entry: " +
                              e.getMessage());
                logger.throwing(CLASS_NAME, "ODCIIndexTruncate", e);
                return ERROR;
            }
        }
        logger.exiting(CLASS_NAME, "ODCIIndexTruncate", SUCCESS);
        return SUCCESS;
    }

    /**
     * Rebuild an existing index
     *
     * @param directoryPrefix
     *            with syntax OWNER.INDEX_NAME
     * @throws SQLException
     */
    public static void rebuild(String directoryPrefix) throws SQLException {
        OJVMDirectory dir = null;
        IndexWriter writer = null;
        OracleCallableStatement cs = null;
        Connection conn;
        logger.entering(CLASS_NAME, "rebuild", directoryPrefix);
        try {
            conn = OJVMUtil.getConnection();
            // get exclusive write lock
            cs = (OracleCallableStatement)conn.prepareCall(StringUtils.replace(lckLuceneTableStmt, "%IDX%", directoryPrefix));
            cs.execute();
            cs.close();
            cs = null;
            dir = OJVMDirectory.getDirectory(directoryPrefix);
            Parameters parameters = Parameters.getParameters(directoryPrefix);
            // Discard pending changes and purge deleted documents
            discard(directoryPrefix);
            // Index parameters
            String columnName = parameters.getParameter("ColName");
            String tableSchema = parameters.getParameter("TableSchema");
            String tableName = parameters.getParameter("TableName");
            String partition = parameters.getParameter("Partition");
            String syncMode = parameters.getParameter("SyncMode", "Deferred");
            logger.info("Rebuilding index on column: '" + columnName + "'");
            logger.info("Internal Parameters:\n" +
                    parameters.toString());
            TableIndexer index =
                new TableIndexer(OJVMUtil.getConnection(), tableSchema,
                                 tableName, partition);
            // open a writer object with last argument to true cause truncation
            // on lucene index
            writer = getIndexWriterForDir(dir, true);
            String extraTabs = parameters.getParameter("ExtraTabs");
            String whereCondition = parameters.getParameter("WhereCondition");
            boolean lockMasterTable =
                "true".equalsIgnoreCase(parameters.getParameter("LockMasterTable", "true"));
            logger.info(" ExtraTabs: " + extraTabs + " WhereCondition: " +
                        whereCondition + " LockMasterTable: " +
                        lockMasterTable);
            index.setExtraTabsStr(extraTabs);
            index.setExtraWhereStr(whereCondition);
            index.setLockMasterTable(lockMasterTable);
            if ("OnLine".equalsIgnoreCase(syncMode)) {
                int batchCount =
                    Integer.parseInt(parameters.getParameter("BatchCount",
                                                             "115"));
                // User Data Store are not relevant here, only enqueue all
                // rowids returned
                // By a full scan of the table
                logger.info("BatchCount: " + batchCount);
                index.setExtraColsStr("null");
                index.index(logger, directoryPrefix, batchCount);
            } else {
                // rebuild the index by execution of TableIndexer.index()
                // User Data Store parameters
                UserDataStore userDataStore = getUserDataStore(parameters);
                boolean includeMasterColumn =
                    "true".equalsIgnoreCase(parameters.getParameter("IncludeMasterColumn",
                                                                    "true"));
                String extraCols = parameters.getParameter("ExtraCols");
                logger.info("ExtraCols: " + extraCols + "User Data Store: " +
                            userDataStore);
                index.setUserDataStore(userDataStore);
                index.setExtraColsStr(extraCols);
                index.index(logger, writer, columnName, includeMasterColumn);
                dir.purge();
            }
            writer.commit();
            logger.exiting(CLASS_NAME, "rebuild", SUCCESS);
        } catch (IOException e) {
            logger.severe("failed to rebuild index: " + directoryPrefix);
            logger.throwing(CLASS_NAME, "rebuild", e);
        } finally {
            try {
                if (writer != null)
                    writer.close();
                writer = null;
                if (dir != null)
                    dir.close();
                dir = null;
            } catch (IOException e) {
                logger.severe("failed to invalidate dir cache entry on index: " +
                              directoryPrefix);
                logger.throwing(CLASS_NAME, "rebuild", e);
            }
        }
    }

    /**
     * Overloading version for XMLType columns
     *
     * @param text
     * @param keyStr
     * @param sortBy
     * @param ctx
     * @param sctx
     * @param scanflg
     * @return
     * @throws SQLException
     * @throws IOException
     */
    public static java.math.BigDecimal TextContains(XMLType text,
                                                    String keyStr,
                                                    String sortBy,
                                                    ODCIIndexCtx ctx,
                                                    LuceneDomainIndex[] sctx,
                                                    java.math.BigDecimal scanflg) throws SQLException,
                                                                                         IOException,
                                                                                         ParseException {
        String valueStr =
            ((text != null) ? DefaultUserDataStore.readStream(new BufferedReader(text.characterStreamValue())) :
             "");
        return TextContains(((valueStr != null) ? valueStr : ""), keyStr,
                            sortBy, ctx, sctx,
                            scanflg); // column value is not used
    }

    /**
     * Overloading version for CLOB columns data type
     *
     * @param text
     * @param keyStr
     * @param sortBy
     * @param ctx
     * @param sctx
     * @param scanflg
     * @return
     * @throws SQLException
     * @throws IOException
     */
    public static java.math.BigDecimal TextContains(CLOB text, String keyStr,
                                                    String sortBy,
                                                    ODCIIndexCtx ctx,
                                                    LuceneDomainIndex[] sctx,
                                                    java.math.BigDecimal scanflg) throws SQLException,
                                                                                         IOException,
                                                                                         ParseException {
        String valueStr =
            (text != null) ? DefaultUserDataStore.readStream(new BufferedReader(text.characterStreamValue())) :
            "";
        return TextContains(((valueStr != null) ? valueStr : ""), keyStr,
                            sortBy, ctx, sctx,
                            scanflg); // column value is not used
    }

    /**
     * Functional implementation for the SQL Operator lcontains(colum,'text to
     * search','sort spec') This entry point is used when lcontains is outside
     * where section, for example: select lcontains(col,'xx') from tabname.
     * sortBy its not relevant at this point because lcontains returns 1 or 0 if
     * the visited rowid contains or not a query string.
     *
     * @param text
     * @param keyStr
     * @param sortBy
     * @param ctx
     * @param sctx
     * @param scanflg
     * @return
     * @throws SQLException
     * @throws IOException
     */
    public static java.math.BigDecimal TextContains(String text, String keyStr,
                                                    String sortBy,
                                                    ODCIIndexCtx ctx,
                                                    LuceneDomainIndex[] sctx,
                                                    java.math.BigDecimal scanflg) throws SQLException,
                                                                                         IOException,
                                                                                         ParseException {

        int flag = scanflg.intValue();
        //logger.entering(CLASS_NAME,"TextContains",new Object []
        //  {text,keyStr,sortBy,ctx,sctx,scanflg});
        ODCIIndexInfo ia = (ctx != null) ? ctx.getIndexInfo() : null;
        //OJVMUtil.dumpIA(logger,ia);
        if (flag == 1 && sctx != null && sctx[0] != null) { 
            // close index operation
            return new BigDecimal("1");
        }
        if (flag == 2 && ctx.getRid() == null) {
          return new BigDecimal("0");
        }
        if (ia == null && flag > 0) { // no Domain Index is bound to a
            // particular column
            SQLException t =
                new SQLException("Column is not indexed by Lucene Domain Index");
            logger.throwing(CLASS_NAME, "TextContains", t);
            throw t;
        }
        String directoryPrefix = getIndexPrefix(ia);
        Entry entry = OJVMDirectory.getCachedDirectory(directoryPrefix);
        TermQuery tq = new TermQuery(new Term("rowid", ctx.getRid()));
        IndexSearcher searcher = entry.getSeacher();
        Parameters pars = entry.getDirectory().getParameters();
        String columnName = pars.getParameter("DefaultColumn");
        QueryParser parser = entry.getParser();
        Query qry = parser.parse(keyStr);
        // since that lcontains is used outside where condition, sort order its
        // not
        // relevant, so use default lucene sort order score,descendent
        String filterKeyStr = qry.toString(columnName);
        Filter docsFilter = null;
        docsFilter = entry.getFilter(filterKeyStr);
        if (docsFilter == null) {
            docsFilter = new QueryWrapperFilter(qry);
            entry.addFilter(filterKeyStr, docsFilter);
            logger.info("storing cachingFilter: " + docsFilter + " key: " +
                        filterKeyStr);
        }
        if (sctx == null || sctx[0] == null) {
            sctx[0] = new LuceneDomainIndex();
            sctx[0].setScanctx(new Integer(1));
        }
        CountHitCollector hitCollector = new CountHitCollector(0);
        searcher.search(tq, docsFilter, hitCollector);
        return BigDecimal.valueOf(hitCollector.getNumHits());
    }

    /**
     * Return a pre-computed value of the lhighlight() for a particular XMLType
     * text.
     *
     * @param text
     * @param keyStr
     * @param ctx
     * @param sctx
     * @param scanflg
     * @return
     * @throws SQLException
     */
    public static String TextHighlight(XMLType text, String keyStr,
                                       String sortBy, ODCIIndexCtx ctx,
                                       LuceneDomainIndex[] sctx,
                                       java.math.BigDecimal scanflg) throws SQLException,
                                                                            IOException,
                                                                            InvalidTokenOffsetsException {
        return TextHighlight(text.getStringVal(), keyStr, sortBy, ctx, sctx,
                             scanflg);
    }

    /**
     * Return a pre-computed value of the lhighlight() for a particular CLOB
     * text.
     *
     * @param text
     * @param keyStr
     * @param ctx
     * @param sctx
     * @param scanflg
     * @return
     * @throws SQLException
     */
    public static String TextHighlight(CLOB text, String keyStr, String sortBy,
                                       ODCIIndexCtx ctx,
                                       LuceneDomainIndex[] sctx,
                                       java.math.BigDecimal scanflg) throws SQLException,
                                                                            IOException,
                                                                            InvalidTokenOffsetsException {
        String valueStr =
            (text != null) ? DefaultUserDataStore.readStream(new BufferedReader(text.characterStreamValue())) :
            "";
        return TextHighlight(((valueStr != null) ? valueStr : ""), keyStr,
                             sortBy, ctx, sctx, scanflg);
    }

    /**
     * Return a pre-computed value of the lhighlight() for a particular String
     * text. We assume that OCIFetch function was called first. lhighlight is
     * know as anciliary operator of lcontains, you can get the score with:
     * select lhighlight(1) from tabname where lcontains(col,'text to find',1)>0
     * last number argument of lcontains match with the argument of lhighlight.
     *
     * @param text
     * @param keyStr
     * @param ctx
     * @param sctx
     * @param scanflg
     * @return
     * @throws SQLException
     */
    public static String TextHighlight(String text, String keyStr,
                                       String sortBy, ODCIIndexCtx ctx,
                                       LuceneDomainIndex[] sctx,
                                       java.math.BigDecimal scanflg) throws SQLException,
                                                                            IOException,
                                                                            InvalidTokenOffsetsException {
        //logger.entering(CLASS_NAME,"TextHighlight", new Object [] {text,keyStr,sortBy,ctx,sctx,scanflg});
        LuceneDomainContext sbtctx;
        int key;
        if (scanflg != null) { // Sanity checks
            SQLException t =
                new SQLException("Can not use lhighlight() without lcontains() in a where side");
            logger.throwing(CLASS_NAME, "TextHighlight", t);
            throw t;
        }
        if (sctx == null || sctx[0] == null) { // Sanity checks
            SQLException t =
                new SQLException("LuceneDomainIndex parameter is null. Are you using lhighlight() in a not index column?");
            logger.throwing(CLASS_NAME, "TextHighlight", t);
            throw t;
        }
        key = sctx[0].getScanctx().intValue();
        // Get the resultSet back from the ContextManager using the key
        sbtctx = (LuceneDomainContext)ContextManager.getContext(key);
        Entry entry = sbtctx.getEntry();
        Analyzer analyzer = entry.getAnalyzer();
        Highlighter highlighter = sbtctx.getHighlighter();
        String fragmentSeparator = sbtctx.getFragmentSeparator();
        int maxNumFragmentsRequired = sbtctx.getMaxNumFragmentsRequired();
        TokenStream tokenStream = null;
        if (highlighter == null) { // Sanity checks
            SQLException t =
                new SQLException("Can not use lhighlight() without lcontains() in a where side");
            logger.throwing(CLASS_NAME, "TextHighlight", t);
            throw t;
        }
        tokenStream =
                analyzer.tokenStream(sbtctx.getColumn(), new StringReader(text));

        String result =
            highlighter.getBestFragments(tokenStream, text, maxNumFragmentsRequired,
                                         fragmentSeparator);
        // logger.exiting(CLASS_NAME,"TextHighlight",result);
        return result;
    }

    /**
     * Return a pre-computed value of the lscore() for a particular rowid. We
     * assume that OCIFetch function was called first and store the score for
     * each rowid visited. lscore is know as anciliary operator of lcontains,
     * you can get the score with: select lscore(1) from tabname where
     * lcontains(col,'text to find',1)>0 last number argument of lcontains match
     * with the argument of lscore.
     *
     * @param text
     * @param keyStr
     * @param ctx
     * @param sctx
     * @param scanflg
     * @return
     * @throws SQLException
     */
    public static java.math.BigDecimal TextScore(String text, String keyStr,
                                                 String sortBy,
                                                 ODCIIndexCtx ctx,
                                                 LuceneDomainIndex[] sctx,
                                                 java.math.BigDecimal scanflg) throws SQLException,
                                                                                      IOException {
        //logger.entering(CLASS_NAME,"TextScore",
        //  new Object [] {text,keyStr,sortBy,ctx,sctx,scanflg});
        LuceneDomainContext sbtctx;
        int key;
        if (scanflg != null) { // Sanity checks
            SQLException t =
                new SQLException("Can not use lscore() without lcontains() in a where side");
            logger.throwing(CLASS_NAME, "TextScore", t);
            throw t;
        }
        if (sctx == null || sctx[0] == null) { // Sanity checks
            SQLException t =
                new SQLException("LuceneDomainIndex parameter is null. Are you using lcontains in a not index column?");
            logger.throwing(CLASS_NAME, "TextScore", t);
            throw t;
        }
        key = sctx[0].getScanctx().intValue();
        // Get the resultSet back from the ContextManager using the key
        sbtctx = (LuceneDomainContext)ContextManager.getContext(key);
        int docId;
        BigDecimal scoreValue;
        if (!sbtctx.isStoreScore())
            throw new SQLException("TextScore do not have pre-computed score values");
        Hashtable cachedRowids = sbtctx.getScoreList();
        if (cachedRowids == null)
            throw new SQLException("TextScore do not have pre-computed cachedRowids");
        Integer cachedDocId = (Integer)cachedRowids.get(ctx.getRid());
        if (cachedDocId == null) {
            SQLException t =
                new SQLException("Ooops, I can't find a pre-cached score with this rowid= '" +
                                 ctx.getRid() + "'");
            logger.severe("Ooops, I can't find a pre-cached score with this rowid= '" +
                          ctx.getRid() + "'");
            logger.throwing(CLASS_NAME, "TextScore", t);
            throw t;
        }
        docId = cachedDocId.intValue();
        IndexScan indexScan = sbtctx.getIndexScan();
        key = sbtctx.getScanContext();
        scoreValue = indexScan.getScoreValue(key,docId);
        //logger.exiting(CLASS_NAME,"TextScore",retVal);
        return scoreValue;
    }

    /**
     * Oracle calls your ODCIStart() method at the beginning of an index scan,
     * passing it information on the index and the operator. Typically, this
     * method: - Initializes data structures used in the scan - Parses and
     * executes SQL statements that query the tables storing the index data -
     * Saves any state information required by the fetch and cleanup methods,
     * and returns the state or a handle to it - Sometimes generates a set of
     * result rows to be returned at the first invocation of ODCIFetch() The
     * information on the index and the operator is not passed to the fetch and
     * cleanup methods. Thus, ODCIStart() must save state data that needs to be
     * shared among the index scan routines and return it through an output sctx
     * parameter. To share large amounts of state data, allocate cursor-duration
     * memory and return a handle to the memory in the sctx parameter. As member
     * methods, ODCIFetch() and ODCIClose() are passed the built-in SELF
     * parameter, through which they can access the state data.
     *
     * @param sctx
     *            IN: The value of the scan context returned by some previous
     *            related query-time call (such as the corresponding ancillary
     *            operator, if invoked before the primary operator); NULL
     *            otherwise OUT: The context that is passed to the next
     *            query-time call; the next query-time call will be to
     *            ODCIIndexFetch
     * @param ia
     *            Contains information about the index and the indexed column
     * @param op
     *            Contains information about the operator predicate
     * @param qi
     *            Contains query information (hints plus list of ancillary
     *            operators referenced)
     * @param strt
     *            The start value of the bounds on the operator return value.
     *            The datatype is the same as that of the operator's return
     *            value
     * @param stop
     *            The stop value of the bounds on the operator return value. The
     *            datatype is the same as that of the operator's return value.
     * @param cmpval
     *            The value arguments of the operator invocation. The number and
     *            datatypes of these arguments are the same as those of the
     *            value arguments to the operator.
     * @param env
     *            The environment handle passed to the routine
     * @return ODCIConst.Success on success, or ODCIConst.Error on error
     * @throws SQLException
     */
    public static java.math.BigDecimal ODCIStart(LuceneDomainIndex[] sctx,
                                                 ODCIIndexInfo ia,
                                                 ODCIPredInfo op,
                                                 ODCIQueryInfo qi,
                                                 java.math.BigDecimal strt,
                                                 java.math.BigDecimal stop,
                                                 java.math.BigDecimal cmppos,
                                                 String cmpval,
                                                 ODCIEnv env) throws java.sql.SQLException,
                                                                     IOException {
        logger.entering(CLASS_NAME, "ODCIStart", cmppos);
        return ODCIStart(sctx, ia, op, qi, strt, stop, cmpval, null, env);
    }

    /**
     * Oracle calls your ODCIStart() method at the beginning of an index scan,
     * passing it information on the index and the operator. Typically, this
     * method: - Initializes data structures used in the scan - Parses and
     * executes SQL statements that query the tables storing the index data -
     * Saves any state information required by the fetch and cleanup methods,
     * and returns the state or a handle to it - Sometimes generates a set of
     * result rows to be returned at the first invocation of ODCIFetch() The
     * information on the index and the operator is not passed to the fetch and
     * cleanup methods. Thus, ODCIStart() must save state data that needs to be
     * shared among the index scan routines and return it through an output sctx
     * parameter. To share large amounts of state data, allocate cursor-duration
     * memory and return a handle to the memory in the sctx parameter. As member
     * methods, ODCIFetch() and ODCIClose() are passed the built-in SELF
     * parameter, through which they can access the state data.
     *
     * @param sctx
     *            IN: The value of the scan context returned by some previous
     *            related query-time call (such as the corresponding ancillary
     *            operator, if invoked before the primary operator); NULL
     *            otherwise OUT: The context that is passed to the next
     *            query-time call; the next query-time call will be to
     *            ODCIIndexFetch
     * @param ia
     *            Contains information about the index and the indexed column
     * @param op
     *            Contains information about the operator predicate
     * @param qi
     *            Contains query information (hints plus list of ancillary
     *            operators referenced)
     * @param strt
     *            The start value of the bounds on the operator return value.
     *            The datatype is the same as that of the operator's return
     *            value
     * @param stop
     *            The stop value of the bounds on the operator return value. The
     *            datatype is the same as that of the operator's return value.
     * @param cmpval
     *            The value arguments of the operator invocation. The number and
     *            datatypes of these arguments are the same as those of the
     *            value arguments to the operator.
     * @param env
     *            The environment handle passed to the routine
     * @return ODCIConst.Success on success, or ODCIConst.Error on error
     * @throws SQLException
     * @see org.apache.lucene.misc.ojvm.HighFreqTerms#TextFreqTerms LFREQTERMS
     *      op
     */
    public static java.math.BigDecimal ODCIStart(LuceneDomainIndex[] sctx,
                                                 ODCIIndexInfo ia,
                                                 ODCIPredInfo op,
                                                 ODCIQueryInfo qi,
                                                 java.math.BigDecimal strt,
                                                 java.math.BigDecimal stop,
                                                 String cmpval, String sortval,
                                                 ODCIEnv env) throws java.sql.SQLException,
                                                                     IOException {
        //logger.entering(CLASS_NAME, "ODCIStart",
        //                new Object[] { sctx, ia, op, qi, strt, stop, cmpval,
        //                               sortval, env });
        if (logger.isLoggable(Level.INFO)) {
            OJVMUtil.dumpIA(logger, ia);
            OJVMUtil.dumpOP(logger, op);
            OJVMUtil.dumpQI(logger, qi);
            OJVMUtil.dumpEnv(logger, env);
        }
        if (!op.getObjectName().equalsIgnoreCase("lcontains")) {
            SQLException t =
                new SQLException("Expected lcontains operator, use lcontains(column,'text to search')>0");
            logger.throwing(CLASS_NAME, "ODCIStart", t);
            throw t;
        }
        String directoryPrefix = getIndexPrefix(ia);
        Entry entry = OJVMDirectory.getCachedDirectory(directoryPrefix);
        Parameters pars = entry.getDirectory().getParameters();
        String sortStr = null;
        int key;
        int qiFlags = qi.getFlags().intValue();
        int numOps = (qi.getAncOps() != null) ? qi.getAncOps().length() : 0;
        boolean highlightText = false;
        boolean storeScore = false;
        for (int i = 0; i < numOps; i++) {
            String opName = qi.getAncOps().getElement(i).getObjectName();
            if ("LSCORE".equals(opName) || "LFREQTERMS".equals(opName))
                storeScore = true;
            if ("LHIGHLIGHT".equals(opName))
                highlightText = true;
        }
        String extraCols = pars.getParameter("ExtraCols");
        if (sortval == null || sortval.length() == 0)
            sortStr = getSortStr(qi, extraCols);
        else
            sortStr = sortval;

        boolean firstRowHint =
            (qiFlags & QUERY_FIRST_ROWS) == QUERY_FIRST_ROWS;
        LuceneDomainContext sbtctx = new LuceneDomainContext();
        String columnName = pars.getParameter("DefaultColumn");
        logger.info("Indexing column: '" + columnName + "'");
        logger.info("Analyzer: " + entry.getAnalyzer());
        sbtctx.setColumn(columnName);
        String tableSchema = ia.getIndexCols().getElement(0).getTableSchema();
        String tableName = ia.getIndexCols().getElement(0).getTableName();
        sbtctx.setMasterTable(tableSchema + "." + tableName);
        String searcherHost = pars.getLuceneRandomSearcher();
        IndexScan indexScan = getSearcher(searcherHost);
        sbtctx.setIndexScan(indexScan);
        String queryString = (cmpval == null) ? "" : cmpval.trim(); // Sanity check
        // inject filter by expresion defined at index creation time
        if (qi.getCompInfo() != null && qi.getCompInfo().getPredInfo() != null) {
            queryString = addFilterByExp(qi.getCompInfo().getPredInfo(), queryString, extraCols);
        }
        key = indexScan.start(directoryPrefix, queryString, sortStr, storeScore, firstRowHint);
        if (highlightText) {
            Query qry = indexScan.getQuery(key);
            String highlightColumn = columnName;
            if (queryString.contains("*")) {
                highlightColumn =
                        pars.getParameter("HighlightColumn", columnName);
            }
            sbtctx.setColumn(highlightColumn);
            Query qryRewrited = qry.rewrite(entry.getReader());
            Formatter formatter = getFormatter(pars);
            int maxNumFragmentsRequired =
                Integer.parseInt(pars.getParameter("MaxNumFragmentsRequired",
                                                   "4"));
            int fragmentSize =
                Integer.parseInt(pars.getParameter("FragmentSize", "100"));
            String fragmentSeparator =
                pars.getParameter("FragmentSeparator", "...");
            Highlighter highlighter =
                new Highlighter(formatter, new QueryScorer(qryRewrited));
            highlighter.setTextFragmenter(new SimpleFragmenter(fragmentSize));
            sbtctx.setHighlighter(highlighter);
            sbtctx.setMaxNumFragmentsRequired(maxNumFragmentsRequired);
            sbtctx.setFragmentSeparator(fragmentSeparator);
        }
        sbtctx.setScanContext(key);
        sbtctx.setEntry(entry);
        sbtctx.setStoreScore(storeScore);
        key = ContextManager.setContext(sbtctx);
        sctx[0] = new LuceneDomainIndex();
        // set the key into the self argument so that we can retrieve the
        // context with this key later.
        sctx[0].setScanctx(new Integer(key));
        //logger.exiting(CLASS_NAME, "ODCIStart", key);
        return SUCCESS;
    }

    /**
     * Oracle calls your ODCIFetch() method to return the row identifiers of the
     * next batch of rows that satisfies the operator predicate, passing it the
     * state data returned by ODCIStart() or the previous ODCIFetch() call. The
     * operator predicate is specified in terms of the operator expression (name
     * and arguments) and a lower and upper bound on the operator return values.
     * Thus, ODCIFetch() must return the row identifiers of the rows for which
     * the operator return value falls within the specified bounds. To indicate
     * the end of index scan, return a NULL.
     *
     * @param nrows
     *            Is the maximum number of result rows that can be returned to
     *            Oracle in this call
     * @param rids
     *            Is the array of row identifiers for the result rows being
     *            returned by this call
     * @param env
     *            The environment handle passed to the routine
     * @return ODCIConst.Success on success, or ODCIConst.Error on error
     * @throws SQLException
     */
    public String ODCIFetch(java.math.BigDecimal nrows,
                                          ODCIRidList[] rids,
                                          ODCIEnv env) throws java.sql.SQLException {
        //logger.entering(CLASS_NAME, "ODCIFetch",
        //                new Object[] { nrows, rids, env });
        LuceneDomainContext sbtctx; // cntxt obj that holds the ResultSet and
        // Statement
        int key = getScanctx().intValue();
        //logger.finest("ContextManager key=" + key);
        // Get the resultSet back from the ContextManager using the key
        sbtctx = (LuceneDomainContext)ContextManager.getContext(key);
        String[] rlist = null;
        try {
            // get a new batch of rows from local/remote searcher
            int rmtCtx = sbtctx.getScanContext();
            IndexScan indexScan = sbtctx.getIndexScan();
            rlist = indexScan.fetch(rmtCtx, nrows);
            if (sbtctx.isStoreScore()) {
                sbtctx.setScoreListCopy(indexScan.getScoreList(rmtCtx));
            }
        } catch (RemoteException e) {
            logger.throwing(CLASS_NAME, "ODCIFetch", e);
            sbtctx.getMasterTable();
        }
        rids[0] = new ODCIRidList(rlist);
        //logger.exiting(CLASS_NAME, "ODCIFetch", rids[0]);
        return sbtctx.getMasterTable();
    }

    /**
     * Oracle calls your ODCIIndexClose() method when the cursor is closed or
     * reused, passing it the current state. ODCIIndexClose() should perform
     * whatever cleanup or closure operations your indextype requires.
     *
     * @param env
     *            The environment handle passed to the routine
     * @return ODCIConst.Success on success, ODCIConst.Error on error
     * @throws SQLException
     */
    public java.math.BigDecimal ODCIClose(ODCIEnv env) throws java.sql.SQLException {
        //logger.entering(CLASS_NAME, "ODCIClose", env);
        LuceneDomainContext sbtctx; // contxt obj that holds the ResultSet and
        // Statement
        int key = getScanctx().intValue();
        sbtctx = (LuceneDomainContext)ContextManager.clearContext(key);
        IndexScan indexScan = sbtctx.getIndexScan();
        try {
          indexScan.close(sbtctx.getScanContext());
        } catch (RemoteException e) {
            logger.throwing(CLASS_NAME, "ODCIClose", e);
            return ERROR;
        }
        return SUCCESS;
    }

    /**
     * Process changes from Lucene Domain Index queue working in
     * SyncMode:Deferred Queue operation are first consumed in PLSQL (faster)
     * then a list of deleted and inserted rowids are sent to this method.
     *
     * @param prefix
     * @throws IOException
     * @throws SQLException
     */
    public static void sync(String prefix, ODCIRidList deleted,
                            ODCIRidList inserted) throws IOException,
                                                         SQLException {
        OracleCallableStatement cs = null;
        Connection conn;
        if (deleted.length() == 0 && inserted.length() == 0) // Sanity checks
            return; // Shorcut ;)
        logger.entering(CLASS_NAME, "sync",
                        new Object[] { prefix, deleted, inserted });
            Parameters par = Parameters.getParameters(prefix);
        boolean onLine = "OnLine".equalsIgnoreCase(par.getParameter("SyncMode", "Deferred"));
        String updaterHost = par.getLuceneUpdater();
        IndexUpdate indexUpdater = localUpdater;
        if (onLine && !"local".equalsIgnoreCase(updaterHost))
            indexUpdater = getUpdater(updaterHost);
        if (indexUpdater == localUpdater) {
            // if not working in OnLine or failed to connect to the remote updater
            conn = OJVMUtil.getConnection();
            cs = (OracleCallableStatement)conn.prepareCall(StringUtils.replace(lckLuceneTableStmt, "%IDX%", prefix));
            cs.execute();
            cs.close();
            cs = null;
        }
        indexUpdater.sync(prefix, deleted.getArray(), inserted.getArray());
        logger.exiting(CLASS_NAME, "sync", prefix);
    }

    /**
     * Optimize Lucene Index, first Lucene Index parameters from parameters
     * storage then call to IndexWriter.optimize method
     *
     * @param directoryPrefix
     * @throws IOException
     * @throws SQLException
     * @see org.apache.lucene.index.IndexWriter#optimize()
     */
    public static void optimize(String directoryPrefix) throws IOException,
                                                               SQLException {
        IndexWriter writer = null;
        OJVMDirectory dir = null;
        OracleCallableStatement cs = null;
        Connection conn;
        logger.entering(CLASS_NAME, "optimize", directoryPrefix);
        try {
            conn = OJVMUtil.getConnection();
            // get exclusive write lock
            cs = (OracleCallableStatement)conn.prepareCall(StringUtils.replace(lckLuceneTableStmt, "%IDX%", directoryPrefix));
            cs.execute();
            cs.close();
            cs = null;
            dir = OJVMDirectory.getDirectory(directoryPrefix);
            // before optimize purge deleted documents
            Parameters par = Parameters.getParameters(directoryPrefix);
            int parDegree =
                Integer.parseInt(par.getParameter("ParallelDegree", "0"));
            Integer maxSegments = new Integer(par.getParameter("MaxSegments", "1"));
            dir.purge();
            writer = getIndexWriterForDir(dir, false);
            // force merge, replace optimize Lucene 4.0.0
            writer.forceMerge(maxSegments);
            writer.commit();
            // after optimize purge deleted documents
            dir.purge();
            // after optimize purge deleted documents on slave storage
            for (int i = 0; i < parDegree; i++) {
                OJVMDirectory slaveDir = OJVMDirectory.getDirectory(directoryPrefix + "$" + i);
                slaveDir.purge();
                slaveDir.close();
                slaveDir = null;
            }
        } catch (IOException e) {
            logger.throwing(CLASS_NAME, "optimize", e);
            throw e;
        } finally {
            try {
                if (writer != null)
                    writer.close();
                writer = null;
                if (dir != null) {
                    dir.close();
                }
                dir = null;
            } catch (IOException e) {
                logger.throwing(CLASS_NAME, "optimize", e);
                throw e;
            }
        }
    }

    /**
     * Discard pending changes on Lucene Domain Index by deleting messages on
     * his queue
     *
     * @param prefix
     */
    public static void discard(String prefix) {
        OracleCallableStatement cs = null;
        Connection conn = null;
        logger.entering(CLASS_NAME, "discard", prefix);
        try {
            conn = OJVMUtil.getConnection();
            cs = (OracleCallableStatement)conn.prepareCall(purgueLuceneQueueStmt);
            cs.setString(1, prefix);
            cs.execute();
            logger.info(".prepareCall :" + purgueLuceneQueueStmt);
            logger.info(".setString :" + prefix);
        } catch (SQLException e) {
            logger.warning(".discard : " + e.getLocalizedMessage());
        } finally {
            OJVMUtil.closeDbResources(cs, null);
        }
    }

    /**
     * Count Hits for in a given index for an specific query Index is using
     * syntax SCHEMA.IDX_NAME Query is a QueryParser syntax without the
     * extension "rownum:[n TO m] AND" order means DESC or ASC, if you use a
     * proper value according to your next query order by clause countHits will
     * pre-cached the query.
     *
     * @param owner
     *            index owner
     * @param indexName
     *            index name
     * @param cmpval
     * @return
     * @throws SQLException
     * @see QueryParser
     */
    public static java.math.BigDecimal countHits(String owner,
                                                 String indexName,
                                                 String cmpval) throws RemoteException {
        String directoryPrefix = owner + "." + indexName;
        logger.entering(CLASS_NAME, "countHits",
                        new Object[] { directoryPrefix, cmpval });
        String searcherHost = Parameters.getParameters(directoryPrefix).getLuceneRandomSearcher();
        IndexScan indexScan = getSearcher(searcherHost);
        BigDecimal totalHits =
            new BigDecimal(indexScan.getNumHits(directoryPrefix, cmpval));
        logger.exiting(CLASS_NAME, "countHits", totalHits);
        return totalHits;
    }

    /**
     * Two parameters during the index creation time of the index can be used to
     * customize the analyzer
     * Analyzer:fully.class.name.toAnalyzer example: create index
     * source_big_lidx on test_source_big(text) indextype is lucene.LuceneIndex
     * parameters('Analyzer:org.apache.lucene.analysis.StopAnalyzer;MergeFactor:1000;MaxBufferedDocs:1000')
     * or PerFieldAnalyzer:PerFieldAnalyzer:line(org.apache.lucene.analysis.core.KeywordAnalyzer),
     *       type(org.apache.lucene.analysis.core.KeywordAnalyzer),
     *       TEXT(org.apache.lucene.analysis.core.SimpleAnalyzer)
     * @param parameters
     *            array extracted from the create index args or from the storage
     * @return an Analyzer instance
     * @throws SQLException
     */
    public static Analyzer getAnalyzer(Parameters parameters) throws SQLException {
        Analyzer analyzer = null;
        try {
            String perFieldStr = parameters.getParameter("PerFieldAnalyzer");
            String analyzerStr;
            Class clazz;
            Map<String,Analyzer> analyzerPerField = new HashMap<String,Analyzer>();
            if (perFieldStr != null && perFieldStr.length() > 0) { // Using
                // PerFieldAnalyzer
                // parameter
                logger.config("PerFieldAnalyzer: '" + perFieldStr + "'");
                analyzerPerField.put("rowid", new KeywordAnalyzer());
                String analyzerStrArr[] = perFieldStr.split(",");
                for (int i = 0; i < analyzerStrArr.length; i++) {
                    int argPos = analyzerStrArr[i].indexOf("(");
                    if (argPos < 0) // Sanity checks
                        throw new SQLException("Invalid format string for PerFieldAnalyzer parameter must be 'colName(analyzerClass)' and was '" +
                                               analyzerStrArr[i] + "'");
                    String colName = analyzerStrArr[i].substring(0, argPos);
                    analyzerStr =
                            analyzerStrArr[i].substring(argPos + 1, analyzerStrArr[i].length() -
                                                        1);
                    clazz = analyzerStr.getClass().forName(analyzerStr);
                    try {
                        Class argsClass[] = new Class[1];
                        argsClass[0] = Version.class;
                        Object argsVal[] = new Object[1];
                        argsVal[0] = LUCENE_COMPAT_VERSION;
                        Constructor ctor = clazz.getConstructor(argsClass);
                        analyzer = (Analyzer)ctor.newInstance(argsVal);
                        logger.config("Analyzer: '" + analyzerStr +
                                      "' for column: '" + colName + "'" +
                                      " Version: '" + LUCENE_COMPAT_VERSION +
                                      "'");
                    } catch (NoSuchMethodException e) {
                        analyzer = (Analyzer)clazz.newInstance();
                        logger.config("Analyzer: '" + analyzerStr +
                                      "' for column: '" + colName + "'" +
                                      " Version: '" + LUCENE_COMPAT_VERSION +
                                      "'");
                    }
                    analyzerPerField.put(colName, analyzer);
                }
                PerFieldAnalyzerWrapper retVal =
                    new PerFieldAnalyzerWrapper(new StandardAnalyzer(LUCENE_COMPAT_VERSION), analyzerPerField);
                logger.exiting(CLASS_NAME, "getAnalyzer", retVal);
                return retVal;
            }
            analyzerStr =
                    parameters.getParameter("Analyzer", DEFAULT_ANALYZER);
            clazz = analyzerStr.getClass().forName(analyzerStr);
            try {
                Class argsClass[] = new Class[1];
                argsClass[0] = Version.class;
                Object argsVal[] = new Object[1];
                argsVal[0] = LUCENE_COMPAT_VERSION;
                Constructor ctor = clazz.getConstructor(argsClass);
                analyzer = (Analyzer)ctor.newInstance(argsVal);
                logger.config("Analyzer: '" + analyzerStr + "'" +
                              " Version: '" + LUCENE_COMPAT_VERSION + "'");
            } catch (NoSuchMethodException e) {
                analyzer = (Analyzer)clazz.newInstance();
                logger.config("Analyzer: '" + analyzerStr + "'" +
                              " Version: '" + LUCENE_COMPAT_VERSION + "'");
            }
        } catch (ClassNotFoundException c) {
            SQLException t = new SQLException(c.getMessage());
            logger.throwing(CLASS_NAME, "getAnalyzer", t);
            throw t;
        } catch (InstantiationException i) {
            SQLException t = new SQLException(i.getMessage());
            logger.throwing(CLASS_NAME, "getAnalyzer", t);
            throw t;
        } catch (IllegalAccessException e) {
            SQLException t = new SQLException(e.getMessage());
            logger.throwing(CLASS_NAME, "getAnalyzer", t);
            throw t;
        } catch (InvocationTargetException e) {
            SQLException t = new SQLException(e.getMessage());
            logger.throwing(CLASS_NAME, "getAnalyzer", t);
            throw t;
        }
        logger.exiting(CLASS_NAME, "getAnalyzer", analyzer);
        return analyzer;
    }

    /**
     * Get a formatter instance used for highlighting.
     *
     * @param parameters
     *            array extracted from the create index args or from the storage
     * @return a Formatter instance
     * @throws SQLException
     */
    public static Formatter getFormatter(Parameters parameters) throws SQLException {
        String formatterClass =
            parameters.getParameter("Formatter", "org.apache.lucene.search.highlight.SimpleHTMLFormatter");
        if (formatterClass == null || formatterClass.length() == 0) // Sanity
            // checks
            return null;
        logger.info("Formatter=" + formatterClass);
        Formatter formatter = null;
        try {
            Class clazz = formatterClass.getClass().forName(formatterClass);
            formatter = (Formatter)clazz.newInstance();
        } catch (ClassNotFoundException c) {
            SQLException t = new SQLException(c.getMessage());
            logger.throwing(CLASS_NAME, "getFormatter", t);
            throw t;
        } catch (InstantiationException i) {
            SQLException t = new SQLException(i.getMessage());
            logger.throwing(CLASS_NAME, "getFormatter", t);
            throw t;
        } catch (IllegalAccessException e) {
            SQLException t = new SQLException(e.getMessage());
            logger.throwing(CLASS_NAME, "getFormatter", t);
            throw t;
        }
        logger.exiting(CLASS_NAME, "getFormatter", formatter);
        return formatter;
    }

    /**
     * This parameters during the index creation time of the index can be used
     * to customize the document retrieval process using a User Defined Function
     * example: create index source_big_lidx on test_source_big(text) indextype
     * is lucene.LuceneIndex parameters('Analyzer:org.apache.lucene.analysis.StopAnalyzer;UserDataStore:com.scotas.lucene.indexer.DefaultUserDataStore;ExtraCols:f3,f4')
     * ; This function must implement the interface
     * com.scotas.lucene.indexer.UserDataStore have an empty contructor and
     * implement the method Field [] getExtraFields(rowid,colsName[],colsVal[])
     * which will return an Array of Lucene Field not including the rowid and
     * column asociated to the index this set of Field will be added to the
     * Lucene Document to be indexed.
     *
     * @param parameters
     * array extracted from the create index args or from the storage
     * @return an Analyzer instance
     * @throws SQLException
     */
    public static UserDataStore getUserDataStore(Parameters parameters) throws SQLException {
        String userDataStoreClass =
            parameters.getParameter("UserDataStore", "com.scotas.lucene.indexer.DefaultUserDataStore");
        if (userDataStoreClass == null ||
            userDataStoreClass.length() == 0) // Sanity
            // checks
            return null;
        logger.info("UserDataStoreClass=" + userDataStoreClass);
        UserDataStore userDataStore = null;
        try {
            Class clazz =
                userDataStoreClass.getClass().forName(userDataStoreClass);
            userDataStore = (UserDataStore)clazz.newInstance();
            String formatCols = parameters.getParameter("FormatCols");
            HashMap formatMaps = new HashMap();
            if (formatCols != null) {
                String[] formatColsArr = formatCols.split(",");
                for (int i = 0; i < formatColsArr.length; i++) {
                    String colFormat = formatColsArr[i];
                    int pos = colFormat.indexOf('(');
                    String colName = colFormat.substring(0, pos);
                    if (pos < 0)
                        throw new RuntimeException("Invalid format in param FormatCols.");
                    else {
                        String format =
                            colFormat.substring(pos + 1, colFormat.length() -
                                                1);
                        logger.info("format for col=" + colName + " '" +
                                    format + "'");
                        formatMaps.put(colName, format);
                    }
                }
            }
            userDataStore.setColumnFormat(formatMaps);
            userDataStore.setConnection(OJVMUtil.getConnection());
        } catch (ClassNotFoundException c) {
            SQLException t = new SQLException(c.getMessage());
            logger.throwing(CLASS_NAME, "getUserDataStore", t);
            throw t;
        } catch (InstantiationException i) {
            SQLException t = new SQLException(i.getMessage());
            logger.throwing(CLASS_NAME, "getUserDataStore", t);
            throw t;
        } catch (IllegalAccessException e) {
            SQLException t = new SQLException(e.getMessage());
            logger.throwing(CLASS_NAME, "getUserDataStore", t);
            throw t;
        }
        logger.exiting(CLASS_NAME, "getUserDataStore", userDataStore);
        return userDataStore;
    }

    public static IndexWriter getIndexWriterForDir(OJVMDirectory dir,
                                                      boolean createEnable) throws SQLException,
                                                                                   CorruptIndexException,
                                                                                   LockObtainFailedException,
                                                                                   IOException {
        IndexWriter writer = null;
        Parameters parameters = dir.getParameters();
        Analyzer analyzer = getAnalyzer(parameters);
        IndexWriterConfig ic = new IndexWriterConfig(LUCENE_COMPAT_VERSION,analyzer);
      
        int mergeFactor =
            Integer.parseInt(parameters.getParameter("MergeFactor",
                                                     "" + LogMergePolicy.DEFAULT_MERGE_FACTOR));
        int maxBufferedDocs =
            Integer.parseInt(parameters.getParameter("MaxBufferedDocs",
                                                     "" + ic.DEFAULT_MAX_BUFFERED_DOCS));
        int maxMergeDocs =
            Integer.parseInt(parameters.getParameter("MaxMergeDocs",
                                                     "" + LogDocMergePolicy.DEFAULT_MAX_MERGE_DOCS));
        int maxBufferedDeleteTerms =
            Integer.parseInt(parameters.getParameter("MaxBufferedDeleteTerms",
                                                     "" +
                                                     ic.DEFAULT_MAX_BUFFERED_DELETE_TERMS));
        boolean useCompountFileName =
            "true".equalsIgnoreCase(parameters.getParameter("UseCompoundFile",
                                                            "false"));
        boolean autoTuneMemory =
            "true".equalsIgnoreCase(parameters.getParameter("AutoTuneMemory",
                                                            "true"));
        if (autoTuneMemory)
          ic.setRAMBufferSizeMB(((OracleRuntime.getJavaPoolSize() /
                                      100) * 50) / (1024 * 1024));
        else {
            ic.setRAMBufferSizeMB(OracleRuntime.getJavaPoolSize() / (1024 * 1024));
            ic.setMaxBufferedDocs(maxBufferedDocs);
        }
        ic.setMaxBufferedDeleteTerms(maxBufferedDeleteTerms);
        if (createEnable)
          ic.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        else
          ic.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
        //int maxFieldLength = Integer.parseInt(parameters.getParameter("MaxFieldLength",
        //                                             "10000"));
        //TODO: use LimitTokenCountAnalyzer instead.
        // ic.setMaxFieldLength(maxFieldLength);
        LogMergePolicy mp;
        if (autoTuneMemory) {
          mp = new LogByteSizeMergePolicy();
        } else {
          mp = new LogDocMergePolicy();
          mp.setMaxMergeDocs(maxMergeDocs);
          mp.setMergeFactor(mergeFactor);
        }
        mp.setUseCompoundFile(useCompountFileName);
        ic.setMergePolicy(mp);
        writer =
          new IndexWriter(dir, ic);
        //if (logger.isLoggable(Level.INFO))
          //TODO: lock for info stream functionality in 4.0.0
        //  writer.setInfoStream(System.out);
        return writer;
    }

    private static void enableOnLineSync(String prefix, int parallelDegree,
                                         boolean indexOnRam,
                                         boolean isMaster) {
        OracleCallableStatement cs = null;
        Connection conn = null;
        try {
            conn = OJVMUtil.getConnection();
            // System.out.println("tableName: "+tableName+" columnName: "+columnName+" prefix: "+prefix);
            cs = (OracleCallableStatement)conn.prepareCall(enableCallBackStmt);
            cs.setString(1,
                         prefix + "|" + parallelDegree + ":" + ((indexOnRam) ?
                                                                "R" : "D"));
            cs.execute();
            cs.close();
            cs = null;
            logger.info(".prepareCall '" + enableCallBackStmt + "'");
            logger.info(".setString '" + prefix + "|" + parallelDegree + ":" +
                        ((indexOnRam) ? "R" : "D") + "'");
            if (isMaster && parallelDegree > 1) {
                cs =
  (OracleCallableStatement)conn.prepareCall(StringUtils.replace(crtLuceneSeqStmt,
                                                                "%IDX%",
                                                                prefix) +
                                            (parallelDegree - 1));
                cs.execute();
                logger.info(".prepareCall '" + crtLuceneSeqStmt + "'");
                logger.info("%IDX% - '" + prefix + "'");
            }
        } catch (SQLException sqe) {
            logger.throwing(CLASS_NAME, "disableOnLineSync", sqe);
        } finally {
            OJVMUtil.closeDbResources(cs, null);
        }
    }

    private static void disableOnLineSync(String prefix, int parallelDegree,
                                          boolean indexOnRam,
                                          boolean isMaster) {
        OracleCallableStatement cs = null;
        Connection conn = null;
        try {
            conn = OJVMUtil.getConnection();
            cs =
  (OracleCallableStatement)conn.prepareCall(disableCallBackStmt);
            cs.setString(1,
                         prefix + "|" + parallelDegree + ":" + ((indexOnRam) ?
                                                                "R" : "D"));
            cs.execute();
            cs.close();
            cs = null;
            logger.info(".prepareCall '" + disableCallBackStmt + "'");
            logger.info(".setString '" + prefix + "|" + parallelDegree + ":" +
                        ((indexOnRam) ? "R" : "D") + "'");
            if (isMaster && parallelDegree > 1) {
                cs =
  (OracleCallableStatement)conn.prepareCall(StringUtils.replace(drpLuceneSeqStmt,
                                                                "%IDX%",
                                                                prefix));
                cs.execute();
                logger.info(".prepareCall '" + drpLuceneSeqStmt + "'");
                logger.info("%IDX% '" + prefix + "'");
            }
        } catch (SQLException sqe) {
            logger.throwing(CLASS_NAME, "disableOnLineSync", sqe);
        } finally {
            OJVMUtil.closeDbResources(cs, null);
        }
    }

    private static void createLuceneStore(String prefix,
                                          String LobStorageParameters) throws SQLException {
        OracleCallableStatement cs = null;
        Connection conn = null;
        try {
            conn = OJVMUtil.getConnection();
            cs = (OracleCallableStatement)conn.prepareCall(crtLuceneTableStmt);
            cs.setString(1, prefix);
            cs.setString(2, LobStorageParameters);
            cs.execute();
            cs.close();
            cs = null;
            logger.info(".prepareCall '" + crtLuceneTableStmt + "'");
            logger.info(".setString '" + prefix + "'");
            logger.info(".setString '" + LobStorageParameters + "'");
            cs = (OracleCallableStatement)conn.prepareCall(crtLuceneQueueStmt);
            cs.setString(1, prefix);
            cs.execute();
            logger.info(".prepareCall '" + crtLuceneQueueStmt + "'");
            logger.info(".setString '" + prefix + "'");
        } catch (SQLException sqe) {
            logger.throwing(CLASS_NAME, "createLuceneStore", sqe);
            throw sqe;
        } finally {
            OJVMUtil.closeDbResources(cs, null);
        }
    }

    private static void dropLuceneStore(String prefix) {
        OracleCallableStatement cs = null;
        Connection conn = null;
        try {
            conn = OJVMUtil.getConnection();
            cs = (OracleCallableStatement)conn.prepareCall(drpLuceneQueueStmt);
            cs.setString(1, prefix);
            cs.execute();
            logger.info(".prepareCall '" + drpLuceneQueueStmt + "'");
            logger.info(".setString '" + prefix + "'");
        } catch (SQLException sqe) {
            logger.throwing(CLASS_NAME, "dropLuceneStore", sqe);
        } finally {
            OJVMUtil.closeDbResources(cs, null);
        }
        try {
            conn = OJVMUtil.getConnection();
            cs = (OracleCallableStatement)conn.prepareCall(drpLuceneTableStmt);
            cs.setString(1, prefix);
            cs.execute();
            logger.info(".prepareCall '" + drpLuceneTableStmt + "'");
            logger.info(".setString '" + prefix + "'");
        } catch (SQLException sqe) {
            logger.throwing(CLASS_NAME, "dropLuceneStore", sqe);
        } finally {
            OJVMUtil.closeDbResources(cs, null);
        }
    }

    /**
     * Change default Log Level value
     *
     * @param logLevel
     * @see Level#parse for more detail of logLevel value
     */
    public static void setLogLevel(String logLevel) {
        logger.setLevel(Level.parse(logLevel));
        logger.config(".setLogLevel '" + logger.getLevel() + "'");
    }

    /**
     * @param hostport with syntax host:port bound using RMI
     * @return an IndexScan RMI server
     */
    public static IndexScan getSearcher(String hostport) {
      if ("local".equalsIgnoreCase(hostport))
        return localSearcher;
      IndexScan remoteSearcher;
      try {
          remoteSearcher = (IndexScan)remoteSearchers.get(hostport);
          if (remoteSearcher == null) {
            remoteSearcher =
                  (IndexScan)Naming.lookup("//" + hostport + "/IndexScanServ");
            remoteSearchers.put(hostport, remoteSearcher);
          }
          logger.info("IndexScanServ is remote: " + hostport + " RMI obj: " + remoteSearcher);
      } catch (Exception e) {
          logger.throwing(CLASS_NAME,
                          "Can't not connect to IndexScanServ using RMI",
                          e);
          remoteSearcher = localSearcher;
          logger.warning("IndexScanServ is local can't connect to: " + hostport);
      }
      return remoteSearcher; 
    }

  /**
   * @param hostport with syntax host:port bound using RMI
   * @return an IndexUpdate RMI server
   */
  public static IndexUpdate getUpdater(String hostport) {
    IndexUpdate remoteUpdater;
    try {
        remoteUpdater = (IndexUpdate)remoteUpdaters.get(hostport);
        if (remoteUpdater == null) {
          remoteUpdater =
                (IndexUpdate)Naming.lookup("//" + hostport + "/IndexUpdateServ");
          remoteSearchers.put(hostport, remoteUpdater);
        }
        logger.info("IndexUpdateServ is remote: " + hostport);
    } catch (Exception e) {
        logger.throwing(CLASS_NAME,
                        "Can't not connect to IndexUpdateServ using RMI",
                        e);
        remoteUpdater = localUpdater;
        logger.warning("IndexUpdateServ is local can't connect to: " + hostport);
    }
    return remoteUpdater; 
  }

    /**
     * Returns a modified version of extraCols adding missing cols
     * used by order by, filter by predicates
     * @param ia
     * @param extraCols
     * @return String using extraCols plus ODCIIndexInfo cols
     */
    private static String getExtraColsFromIA(ODCIIndexInfo ia, String extraCols) throws SQLException {
        ODCIColInfo cols[] = ia.getIndexCols().getArray();
        String str = extraCols;
        String extraColsArr[] = extraCols.split(",");
        for (int i=1;i<cols.length;i++) { // Skip first col, master column of the index
            String colName = cols[i].getColName().replaceAll("\"", "");
            int j=0;
            while(j<extraColsArr.length) {
                String arrNameAlias[] = extraColsArr[j].trim().split("\\s+");
                String col = arrNameAlias[0].trim();
                if (colName.equalsIgnoreCase(col))
                    break; // found
                j++;
            }
            if (j == extraColsArr.length)
                str = str + "," + colName + " \"" + colName + "\"";
        }
        if (str.startsWith(","))
            str = str.substring(1);
        logger.info("ExtraCols: " + str);
        return str;
    }
    
    /**
     * Modify queryString with information sent by the RDBMS
     * Pushed Down Predicates, for example:
     *      col < val            => alias:[* TO val}, Flags:0, Stop:  val
     *      col >  val           => alias:[val TO *], Flags:0, Start: val
     *      col >= val           => alias:[val TO *], Flags:4, Start: val
     *      col <= val           => alias:[* TO val}, Flags:8, Stop:  val
     *      col between n AND m  => alias:[n TO m], eq [col >= val and col <= val]
     *      col = val            => alias:(val), Flags:8|4|1, Start|Stop: val
     *      col <> val           => -alias:(val), Flags:256, Start|Stop: val
     * @param pred
     * @param queryString
     * @return String using ODCIFilterInfoList flags
     */
    private static String addFilterByExp(ODCIFilterInfoList pred, String queryString, String extraCols) throws SQLException {
        ODCIFilterInfo filters[] = pred.getArray();
        //System.out.println("extraCols: " + extraCols);
        String strQry = queryString;
        for (int i=0;i<filters.length;i++) { // find alias in ExtraCols Parameter
            ODCIColInfo col = filters[i].getColInfo();
            AnyData start = filters[i].getStrt();
            AnyData stop = filters[i].getStop();
            String colName = col.getColName().replaceAll("\"", "");
            String aliasName = "";
            //System.out.print("col: " + colName + " ");
            String extraColsArr[] = extraCols.split(",");
            int j=0;
            while(j<extraColsArr.length) {
                String arrNameAlias[] = extraColsArr[j].trim().split("\\s+");
                String colStr = arrNameAlias[0];
                if (colName.equalsIgnoreCase(colStr)) {
                    aliasName = arrNameAlias[1].replaceAll("\"", "");
                    break; // found
                }
                j++;
            }
            if (j == extraColsArr.length)
                throw new SQLException("addFilterByExp: Internal error, col: '" + 
                                       colName + "' not in ExtraCols:\n" + extraCols);
            int flags = filters[i].getFlags().intValue();
            if ((flags & PredNotEqual) == PredNotEqual) {
                // AND -alias:"val"
                strQry = strQry + " AND -" + aliasName + ":\"" + OJVMUtil.getAnyDataValue(stop) + "\"";
            } else if ((flags & (PredExactMatch|PredIncludeStart|PredIncludeStop)) 
                       == (PredExactMatch|PredIncludeStart|PredIncludeStop)) {
                // AND alias:(val)
                strQry = strQry + " AND " + aliasName + ":\"" + OJVMUtil.getAnyDataValue(start) + "\"";
            } else if (flags == 0 && start != null) {
                // AND alias:{val TO *}
                strQry = strQry + " AND " + aliasName + ":{" + OJVMUtil.getAnyDataValue(start) + " TO *}";
            } else if (flags == 0 && stop != null) {
                // AND alias:{* TO val}
                strQry = strQry + " AND " + aliasName + ":{* TO " + OJVMUtil.getAnyDataValue(stop) + "}";
            } else if ((flags & PredIncludeStart) == PredIncludeStart) {
                // AND alias:[val TO *]
                strQry = strQry + " AND " + aliasName + ":[" + OJVMUtil.getAnyDataValue(start) + " TO *]";
            } else if ((flags & PredIncludeStop) == PredIncludeStop) {
                // AND alias:[* TO val]
                strQry = strQry + " AND " + aliasName + ":[* TO " + OJVMUtil.getAnyDataValue(stop) + "]";
            }
        }
        if (strQry.startsWith(" AND "))
            strQry = strQry.substring(5);
        logger.info("new queryString: " + strQry);
        return strQry;
    }

    /**
     * Sort string could be defined using 
     * order by sscore() [asc|desc] (traditional domain index way)
     * or using information provided composite domain index
     * order by col1 [asc|desc], col2 [asc|desc], ..
     * @param qi
     * @param extraCols
     * @return String using QueryInfo flags
     */
    private static String getSortStr(ODCIQueryInfo qi, String extraCols) throws SQLException {
        String sortStr;
        int qiFlags = qi.getFlags().intValue();
        ODCICompQueryInfo compQry = qi.getCompInfo();
        if (compQry != null && compQry.getObyInfo() != null && compQry.getObyInfo().length()>0) {
            sortStr = "";
            ODCIOrderByInfoList obyLst = compQry.getObyInfo();
            ODCIOrderByInfo arr[] = obyLst.getArray();
            String extraColsArr[] = extraCols.split(",");
            for (int i=0;i<arr.length;i++) {
                ODCIOrderByInfo obyInfo = arr[i];
                if (obyInfo.getExprType().intValue() == 2) {
                    // ExprType == 2 lscore operator
                    if (obyInfo.getSortOrder().intValue() == 1)
                        sortStr = "ASC";
                    else
                        sortStr = "DESC";
                } else
                    for (int j=0;j<extraColsArr.length;j++) {
                        String arrNameAlias[] = extraColsArr[j].trim().split("\\s+");
                        String col = arrNameAlias[0];
                        String alias = arrNameAlias[1].replaceAll("\"", "");
                        System.out.println("col: " + col + " alias: " + alias);
                        if (col.equalsIgnoreCase(obyInfo.getExprName())) {
                            //TODO: check FormatCols argument to verify int,float,string sort impl.
                            if (obyInfo.getSortOrder().intValue() == 1)
                                sortStr = sortStr + "," + alias + ":ASC";
                            else
                                sortStr = sortStr + "," + alias + ":DESC";
                        }
                    }
            }
            if (sortStr.startsWith(","))
                sortStr = sortStr.substring(1);
        } else
            sortStr =
                  (((qiFlags & QUERY_SORT_ASC) == QUERY_SORT_ASC) ? "ASC" :
                   "DESC"); // no lcontains(col,qry,sort) option, use ODCI flags
        logger.info("Computed sort string: " + sortStr);
        return sortStr;
    }
}
