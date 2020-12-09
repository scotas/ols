package com.scotas.solr.odci;


import com.scotas.lucene.indexer.ContextManager;
import com.scotas.lucene.indexer.DefaultUserDataStore;
import com.scotas.lucene.indexer.Parameters;
import com.scotas.lucene.store.OJVMDirectory;
import com.scotas.lucene.store.OJVMUtil;
import com.scotas.lucene.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.math.BigDecimal;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import java.net.URLEncoder;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.ODCI.AnyData;
import oracle.ODCI.ODCIColInfo;
import oracle.ODCI.ODCICompQueryInfo;
import oracle.ODCI.ODCIEnv;
import oracle.ODCI.ODCIFilterInfo;
import oracle.ODCI.ODCIFilterInfoList;
import oracle.ODCI.ODCIIndexCtx;
import oracle.ODCI.ODCIIndexInfo;
import oracle.ODCI.ODCIOrderByInfo;
import oracle.ODCI.ODCIOrderByInfoList;
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
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.LogDocMergePolicy;
import org.apache.lucene.index.LogMergePolicy;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.JavaBinCodec;
import org.apache.solr.common.util.SimpleOrderedMap;

import org.slf4j.LoggerFactory;


public class SolrDomainIndex implements CustomDatum, CustomDatumFactory {
    static final String CLASS_NAME = SolrDomainIndex.class.getName();

    private static org.slf4j.Logger logger =
        LoggerFactory.getLogger(CLASS_NAME);

    private static final Random RANDOM = new Random();

    static final String enableCallBackStmt =
        "call SolrDomainIndex.enableCallBack(?)";

    static final String disableCallBackStmt =
        "call SolrDomainIndex.disableCallBack(?)";

    static final String crtLuceneTableStmt =
        "call SolrDomainIndex.createTable(?,?)";

    static final String drpLuceneTableStmt =
        "call SolrDomainIndex.dropTable(?)";

    static final String crtLuceneQueueStmt =
        "call LuceneDomainAdm.createQueue(?)";

    static final String crtLuceneSeqStmt =
        "create sequence %IDX%$S cycle nocache minvalue 0 maxvalue ";

    static final String resSolrInsertTableStmt = "insert into %IDX%$T (\n" +
        "  select path(1),extractValue(res,'/Resource/ModificationDate'),dbms_lob.getLength(extractValue(res,'/Resource/XMLLob')),extractValue(res,'/Resource/XMLLob'),'N' from resource_view\n" +
        "      where under_path(res,'/public/solr/%IDX%/conf/',1)>0)";

    static final String resSolrUpdateTableStmt = "update %IDX%$T set\n" +
        "  data=xdburitype('/public/solr/%IDX%/conf/'||name).getBlob(),\n" +
        "  file_size=dbms_lob.getLength(xdburitype('/public/solr/%IDX%/conf/'||name).getBlob()),\n" +
        "  last_modified=sysdate\n" +
        "  where name like '%.xml' or name like '%.txt'";

    static final String allRowidsTableStmt = "DECLARE\n" +
        "CURSOR c1 IS select rowid from %TBL%;\n" +
        "ridlist         sys.odciridlist;\n" +
        "v_limit         NUMBER := ?;\n" +
        "BEGIN\n" +
        "  OPEN c1;\n" +
        "  LOOP\n" +
        "    FETCH c1 BULK COLLECT INTO ridlist LIMIT v_limit;\n" +
        "    SolrDomainIndex.syncInternal(?,sys.ODCIRidList(),ridlist);\n" +
        "    EXIT WHEN ridlist.COUNT = 0;\n" +
        "  END LOOP;\n" +
        "  CLOSE c1;\n" +
        "END;";

    static final String allRowidsEnqueueStmt = "DECLARE\n" +
        "CURSOR c1 IS select rowid from %TBL%;\n" +
        "ridlist         sys.odciridlist;\n" +
        "v_limit         NUMBER := ?;\n" +
        "BEGIN\n" +
        "  OPEN c1;\n" +
        "  LOOP\n" +
        "    FETCH c1 BULK COLLECT INTO ridlist LIMIT v_limit;\n" +
        "    SolrDomainIndex.enqueueChange(?,ridlist,'insert');\n" +
        "    EXIT WHEN ridlist.COUNT = 0;\n" +
        "  END LOOP;\n" +
        "  CLOSE c1;\n" +
        "END;";

    static final String drpLuceneQueueStmt =
        "call LuceneDomainAdm.dropQueue(?)";

    static final String purgueLuceneQueueStmt =
        "call LuceneDomainAdm.purgueQueue(?)";

    static final String drpLuceneSeqStmt = "drop sequence %IDX%$S";

    static final String selFlashbackServStmt = "select host_name,port from LUCENE.FB_PROCESS order by query_time";

    public static final String _SQL_NAME = "LUCENE.SOLRDOMAININDEX";

    public static final int _SQL_TYPECODE = OracleTypes.STRUCT;

    static final String defaultLobParameters =
        "PCTVERSION 0 DISABLE STORAGE IN ROW CACHE READS NOLOGGING";

    static final java.math.BigDecimal SUCCESS = new java.math.BigDecimal("0");

    static final java.math.BigDecimal ERROR = new java.math.BigDecimal("1");

    static final int TRUE = 1;

    static final int FALSE = 0;

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
    static final BigDecimal ODCIConstFirstCall = new BigDecimal(1);

    /**
     * from ODCIConst.IntermediateCall
     */
    static final BigDecimal ODCIConstIntermediateCall = new BigDecimal(2);

    /**
     * from ODCIConst.FinalCall
     */
    static final BigDecimal ODCIConstFinalCall = new BigDecimal(3);

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
    static Pattern patternColAliasList = Pattern.compile("(?:^|,)((?:[^,()]|\\([^()]*\\))*)");
    
    public static final Version LUCENE_COMPAT_VERSION = Version.LUCENE_40;

    static int[] _sqlType = { 4 };

    static CustomDatumFactory[] _factory = new CustomDatumFactory[1];

    MutableStruct _struct;

    static final SolrDomainIndex _SolrDomainIndexFactory =
        new SolrDomainIndex();

    public static CustomDatumFactory getFactory() {
        return _SolrDomainIndexFactory;
    }

    /* constructor */

    public SolrDomainIndex() {
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
        SolrDomainIndex o = new SolrDomainIndex();
        o._struct = new MutableStruct((STRUCT)d, _sqlType, _factory);
        return o;
    }

    /* shallow copy method: give object same attributes as argument */

    void shallowCopy(SolrDomainIndex d) throws SQLException {
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
                    ia.getIndexSchema() + "." + ia.getIndexName() + "$" + ia.getIndexPartition();
        else
            indexPrefix = ia.getIndexSchema() + "." + ia.getIndexName();
        return indexPrefix;
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
        logger.info("ODCIIndexCreate: " + getIndexPrefix(ia));
        OJVMDirectory dir = null;
        IndexWriter writer = null;
        Parameters parameters = new Parameters();
        logger.trace("ODCIIndexCreate", new Object[] { ia, parms, env });
        if (env != null && env.getCallProperty() != null)
            // Partitioning suppport ignore first and last call iteration
            if (env.getCallProperty().equals(ODCIConstFirstCall) ||
                env.getCallProperty().equals(ODCIConstFinalCall))
                return SUCCESS;
        OracleCallableStatement cs = null;
        Connection conn;
        try {
            String directoryPrefix = getIndexPrefix(ia);
            if (parms != null) {
                String[] parmList = parms.split(";");
                for (int i = 0; i < parmList.length; i++) {
                    String[] nameValue = parmList[i].split(":", 2);
                    String name = nameValue[0];
                    if (name.startsWith("~"))
                        continue;
                    if (!Parameters.isValidParameterName(nameValue[0])) {
                        logger.error("Not validad parameter name: " + name);
                        logger.trace("ODCIIndexCreate", ERROR);
                        return ERROR;
                    }
                    if (name.equalsIgnoreCase("LogLevel")) {
                        Logger julLogger = Logger.getLogger(CLASS_NAME);
                        julLogger.setLevel(Level.parse(nameValue[1]));
                        if (logger.isTraceEnabled()) {
                            OJVMUtil.dumpIA(julLogger, ia);
                            OJVMUtil.dumpEnv(julLogger, env);
                        }
                    }
                    parameters.setParameter(name, nameValue[1]);
                    logger.info("store parameter : '" + nameValue[0] +
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
            logger.info("Internal Parameters (" + directoryPrefix + "):\n" +
                    parameters.toString());
            int batchCount =
                Integer.parseInt(parameters.getParameter("BatchCount",
                                                         "32767"));
            boolean indexOnRam =
                "true".equalsIgnoreCase(parameters.getParameter("IndexOnRam",
                                                                "false"));
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
            dir = OJVMDirectory.getDirectory(directoryPrefix);
            dir.saveParameters(parameters);
            conn = dir.getConnection();
            // create master storage
            writer = getIndexWriterForDir(dir, true);
            writer.commit();
            writer.waitForMerges();
            writer.close();
            writer = null;
            // upload Solr config files from XMLDB repo at /public/solr/LUCENE.SOURCE_SMALL_SIDX/conf/
            cs = (OracleCallableStatement)conn.prepareCall(StringUtils.replace(resSolrInsertTableStmt,
                                                                "%IDX%",
                                                                directoryPrefix));
            int rowsInserted = cs.executeUpdate();
            cs.close();
            cs = null;
            if (rowsInserted == 0)
                Parameters.saveSolrDefaultConfig(directoryPrefix);
            dir.close();
            dir = null;
            conn.commit(); // commit resources to slave process
            if ("OnLine".equalsIgnoreCase(syncMode)) // Finally enable AQ
                // Callback
                enableOnLineSync(directoryPrefix, indexOnRam, true);
            conn.commit(); // commit resources to slave process
            String allTbl;
            if (partition != null && partition.length() > 0)
                allTbl = tableSchema + "." + tableName + " partition("+partition+")";
            else
                allTbl = tableSchema + "." + tableName;
            if (populateIndex && "OnLine".equalsIgnoreCase(syncMode)) {
                // enqueue batch of 32767 rowids for inserting
                logger.info("BatchCount: " + batchCount);
                cs = (OracleCallableStatement)conn.prepareCall(StringUtils.replace(allRowidsEnqueueStmt,"%TBL%",allTbl));
                cs.setInt(1, batchCount);
                cs.setString(2, directoryPrefix);
                logger.info("Enqueue indexing on table: " + tableSchema + "." +
                            tableName + " partition: " + partition + " index: " + directoryPrefix);
                cs.executeUpdate();
                cs.close();
                cs = null;
            } else if (populateIndex) {
                // send batch of 32767 rowids for inserting
                logger.info("BatchCount: " + batchCount);
                cs = (OracleCallableStatement)conn.prepareCall(StringUtils.replace(allRowidsTableStmt,"%TBL%",allTbl));
                cs.setInt(1, batchCount);
                cs.setString(2, directoryPrefix);
                logger.info("Start indexing on table: " + tableSchema + "." +
                            tableName + " partition: " + partition + " index: " + directoryPrefix);
                cs.executeUpdate();
                cs.close();
                cs = null;
            }
        } catch (SQLException e) {
            logger.error("failed to create index: " + e.getMessage());
            logger.trace("ODCIIndexCreate", e);
            return ERROR;
        } catch (IOException e) {
            logger.error("failed to create index: " + e.getMessage());
            logger.trace("ODCIIndexCreate", e);
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
                logger.error("failed to create index: " + e.getMessage());
                logger.trace("ODCIIndexCreate", e);
                return ERROR;
            }
        }
        logger.trace("ODCIIndexCreate", SUCCESS);
        return SUCCESS;
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
        logger.trace("ODCIIndexAlter",
                     new Object[] { ia, parms, option, env });
        String directoryPrefix = getIndexPrefix(ia);
        Parameters parameters = Parameters.getParameters(directoryPrefix);
        Connection conn = OJVMUtil.getConnection();
        OracleCallableStatement cs = null;
        try {
            String logLevel = parameters.getParameter("LogLevel", "WARNING");
            Logger julLogger = Logger.getLogger(CLASS_NAME);
            julLogger.setLevel(Level.parse(logLevel));
            if (logger.isTraceEnabled()) {
                OJVMUtil.dumpIA(julLogger, ia);
                OJVMUtil.dumpEnv(julLogger, env);
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
                // remove this directory from cache
                notifySearchers(directoryPrefix, true);
                notifyUpdater(directoryPrefix);
                String[] parmList;
                if (parms != null && parms[0] != null) {
                    String paramstr = parms[0];
                    parmList = paramstr.split(";");
                } else
                    parmList = new String[0];
                for (int i = 0; i < parmList.length; i++) {
                    String[] nameValue = parmList[i].split(":", 2);
                    String name = nameValue[0];
                    String value = nameValue[1];
                    boolean indexOnRam =
                        "true".equalsIgnoreCase(parameters.getParameter("IndexOnRam",
                                                                        "false"));
                    if (name.startsWith("~")) { // If parameter name start with
                        // ~ means remove it
                        name = name.substring(1);
                        if (!Parameters.isValidParameterName(name)) {
                            logger.error("Not validad parameter name: " +
                                         name);
                            logger.trace("ODCIIndexAlter", ERROR);
                            return ERROR;
                        }
                        if ("SyncMode".equalsIgnoreCase(name))
                            disableOnLineSync(directoryPrefix,
                                              indexOnRam, true);
                        if ("LogLevel".equalsIgnoreCase(name))
                            julLogger.setLevel(Level.parse("WARNING"));
                        parameters.removeParameter(name);
                        logger.info("remove parameter : '" + name + "'");
                    } else {
                        if (!Parameters.isValidParameterName(name)) {
                            logger.error("Not validad parameter name: " +
                                         nameValue[0]);
                            logger.trace("ODCIIndexAlter", ERROR);
                            return ERROR;
                        }
                        parameters.setParameter(name, value);
                        logger.info("store parameter : '" + name +
                                    "' with value '" + value + "'");
                        if ("SyncMode".equalsIgnoreCase(name))
                            if ("OnLine".equalsIgnoreCase(value))
                                enableOnLineSync(directoryPrefix,
                                                 indexOnRam, true);
                            else
                                disableOnLineSync(directoryPrefix,
                                                  indexOnRam, true);
                        if ("LogLevel".equalsIgnoreCase(name))
                            julLogger.setLevel(Level.parse(value));
                    }
                }
            } else {
                logger.error("ODCIIndexAlter error parameter is null or option not implemented. Option: " +
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
            try {
                // update Solr config files from XMLDB repo at /public/solr/LUCENE.SOURCE_SMALL_SIDX/conf/
                cs =
                (OracleCallableStatement)conn.prepareCall(
                                      StringUtils.replace(resSolrUpdateTableStmt,
                                                                "%IDX%",
                                                                directoryPrefix));
                cs.execute();
                cs.close();
                cs = null;
            } catch (SQLException sqe) {
                logger.warn("There where errors fetching Solr conf. files from XMLDB repository, using default templates");
            }
            conn.commit(); // commit resource to slave process
            if (option.equals(ODCIConstAlterIndexRebuild) ||
                option.equals(ODCIConstAlterIndexRebuildOnLine)) {
                try {
                    rebuild(directoryPrefix);
                } catch (SQLException e) {
                    logger.error("failed to rebuild index: " + e);
                    logger.trace("ODCIIndexAlter", ERROR);
                    return ERROR;
                }
            }
            logger.info("Parameters:\n" +
                    parameters.toString());
            // return only internals paramaters to be stored in SYSTEM's views
            parms[0] = parameters.toString();
        } catch (SQLException e) {
            logger.error("failed to alter index: " + e.getMessage());
            logger.trace("ODCIIndexAlter", e);
            return ERROR;
        }
        logger.trace("ODCIIndexAlter", SUCCESS);
        return SUCCESS;
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
        logger.trace("ODCIIndexTruncate", new Object[] { ia, env });
        //OJVMUtil.dumpIA(logger, ia);
        //OJVMUtil.dumpEnv(logger, env);
        if (env != null && env.getCallProperty() != null)
            // Partitioning suppport ignore first and last call iteration
            if (env.getCallProperty().equals(ODCIConstFirstCall) ||
                env.getCallProperty().equals(ODCIConstFinalCall))
                return SUCCESS;
        try {
            notifySearchers(directoryPrefix, true);
            notifyUpdater(directoryPrefix);
            dir = OJVMDirectory.getDirectory(directoryPrefix);
            // remove this directory from cache
            // Discard pending changes and purge deleted documents
            discard(directoryPrefix);
            // open a writer object with last argument to true cause truncation
            // on lucene index
            IndexWriter writer = getIndexWriterForDir(dir, true);
            writer.close();
            writer = null;
        } catch (IOException e) {
            logger.error("failed to truncate index: " + e.getMessage());
            logger.trace("ODCIIndexTruncate", e);
            return ERROR;
        } finally {
            try {
                if (dir != null)
                    dir.close();
                dir = null;
            } catch (IOException e) {
                logger.error("failed to invalidate core: " + e.getMessage());
                logger.trace("ODCIIndexTruncate", e);
            }
        }
        logger.trace("ODCIIndexTruncate", SUCCESS);
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
        logger.trace("ODCIIndexDrop", new Object[] { ia, env });
        if (env != null && env.getCallProperty() != null)
            // Partitioning suppport ignore first and last call iteration
            if (env.getCallProperty().equals(ODCIConstFirstCall) ||
                env.getCallProperty().equals(ODCIConstFinalCall))
                return SUCCESS;
        String directoryPrefix = getIndexPrefix(ia);
        Parameters parameters;
        try {
            parameters = Parameters.getParameters(directoryPrefix);
        } catch (InstantiationError ie) {
            logger.warn("Index parameters not found, may be index doesn't exists: " + directoryPrefix);
            return SUCCESS;
        }
        boolean indexOnRam =
            "true".equalsIgnoreCase(parameters.getParameter("IndexOnRam",
                                                            "false"));
        String logLevel = parameters.getParameter("LogLevel", "WARNING");
        Logger julLogger = Logger.getLogger(CLASS_NAME);
        julLogger.setLevel(Level.parse(logLevel));
        if (logger.isTraceEnabled()) {
            OJVMUtil.dumpIA(julLogger, ia);
            OJVMUtil.dumpEnv(julLogger, env);
        }
        if ("OnLine".equalsIgnoreCase(parameters.getParameter("SyncMode"))) {
            disableOnLineSync(directoryPrefix, indexOnRam, true);
        }
        // remove this directory from cache
        discard(directoryPrefix);
        notifySearchers(directoryPrefix, true);
        notifyUpdater(directoryPrefix);
        dropLuceneStore(directoryPrefix);
        logger.trace("ODCIIndexDrop", SUCCESS);
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
        logger.trace("ODCIIndexMergePartition", new Object[] { ia, env, p1, p2 , parms });
        BigDecimal retVal;
        Parameters pars = Parameters.getParameters(getIndexPrefix(ia));
        // creating new partitions leave it unusable, user should call them rebuild
        pars.setParameter("PopulateIndex", "false");
        String userParameters = pars.getUserParameters();
        logger.info("Partition parameters: " + userParameters);
        logger.info("Dropping index partition: " + 
                    ia.getIndexPartition() + " on table partition: " +
                    ia.getIndexCols().getElement(0).getTablePartition());
        retVal = ODCIIndexDrop(ia, env);
        if (ERROR.equals(retVal))
            return retVal;
        logger.info("setIndexPartition (drop): " + p1.getIndexPartition());
        ia.setIndexPartition(p1.getIndexPartition());
        retVal = ODCIIndexDrop(ia, env);
        if (ERROR.equals(retVal))
            return retVal;
        logger.info("setIndexPartition (create): " + p2.getIndexPartition());
        ia.setIndexPartition(p2.getIndexPartition());
        retVal = ODCIIndexCreate(ia, userParameters, env);
        logger.trace("ODCIIndexMergePartition", retVal);
        return SUCCESS;
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
        logger.trace("ODCIIndexSplitPartition", new Object[] { ia, env, p1, p2 , parms });
        BigDecimal retVal;
        Parameters pars = Parameters.getParameters(getIndexPrefix(ia));
        // creating new partitions leave it unusable, user should call them rebuild
        pars.setParameter("PopulateIndex", "false");
        String userParameters = pars.getUserParameters();
        logger.info("Partition parameters: " + userParameters);
        logger.info("Dropping index partition: " + 
                    ia.getIndexPartition() + " on table partition: " +
                    ia.getIndexCols().getElement(0).getTablePartition());
        retVal = ODCIIndexDrop(ia, env);
        if (ERROR.equals(retVal))
            return retVal;
        logger.info("setIndexPartition (create): " + p1.getIndexPartition());
        ia.setIndexPartition(p1.getIndexPartition());
        retVal = ODCIIndexCreate(ia, userParameters, env);
        if (ERROR.equals(retVal))
            return retVal;
        logger.info("setIndexPartition (create): " + p2.getIndexPartition());
        ia.setIndexPartition(p2.getIndexPartition());
        retVal = ODCIIndexCreate(ia, userParameters, env);
        logger.trace("ODCIIndexSplitPartition", retVal);
        return SUCCESS;
    }

    /**
     * Return a pre-computed value of the shighlight() for a particular XMLType
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
                                       SolrDomainIndex[] sctx,
                                       java.math.BigDecimal scanflg) throws SQLException,
                                                                            IOException,
                                                                            InvalidTokenOffsetsException {
        return TextHighlight(text.getStringVal(), keyStr, sortBy, ctx, sctx,
                             scanflg);
    }

    /**
     * Return a pre-computed value of the shighlight() for a particular CLOB
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
                                       SolrDomainIndex[] sctx,
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
     * Return a pre-computed value of the shighlight() for a particular String
     * text. We assume that OCIFetch function was called first. shighlight is
     * know as anciliary operator of lcontains, you can get the score with:
     * select shighlight(1) from tabname where lcontains(col,'text to find',1)>0
     * last number argument of lcontains match with the argument of shighlight.
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
                                       SolrDomainIndex[] sctx,
                                       java.math.BigDecimal scanflg) throws SQLException,
                                                                            IOException,
                                                                            InvalidTokenOffsetsException {
        logger.trace("TextHighlight",
                     new Object[] { text, keyStr, sortBy, ctx, sctx,
                                    scanflg });
        SolrIndexContext sbtctx;
        int key;
        if (scanflg != null) { // Sanity checks
            SQLException t =
                new SQLException("Can not use SHighlight() without scontains() in a where side");
            logger.error("TextHighlight", t);
            throw t;
        }
        if (sctx == null || sctx[0] == null) { // Sanity checks
            SQLException t =
                new SQLException("SolrDomainIndex parameter is null. Are you using scontains in a not index column?");
            logger.error("TextHighlight", t);
            throw t;
        }
        key = sctx[0].getScanctx().intValue();
        // Get the resultSet back from the ContextManager using the key
        sbtctx = (SolrIndexContext)ContextManager.getContext(key);
        if (!sbtctx.isStoreHighlight())
            throw new SQLException("TextHighlight do not have pre-computed score values");
        Hashtable highlight = sbtctx.getHighlightListCopy();
        SimpleOrderedMap val = (SimpleOrderedMap)highlight.get(ctx.getRid());
        if (val == null)
            throw new SQLException("TextHighlight can't get a highlight text for rowid= " +
                                   ctx.getRid());
        String result;
        String[] hCols = sbtctx.getHighlightCol().split(",");
        ArrayList lines = new ArrayList();
        for (int i = 0; i < hCols.length; i++) {
            ArrayList hl = (ArrayList)val.get(hCols[i]);
            if (hl != null)
                lines.addAll(hl);
        }
        if (lines.isEmpty())
            result = "";
        else
          result =
              StringUtils.join(lines,
                               "\n");
        logger.trace("TextHighlight", result);
        return result.toString();
    }

    /**
     * Return a pre-computed value of the smlt() for a particular CLOB
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
    public static oracle.ODCI.ODCIRidList TextMlt(CLOB text, String keyStr, String sortBy,
                                                  ODCIIndexCtx ctx,
                                                  SolrDomainIndex[] sctx,
                                                  java.math.BigDecimal scanflg) throws SQLException,
                                                                                       IOException,
                                                                                       InvalidTokenOffsetsException {
        String valueStr =
            (text != null) ? DefaultUserDataStore.readStream(new BufferedReader(text.characterStreamValue())) :
            "";
        return TextMlt(((valueStr != null) ? valueStr : ""), keyStr,
                             sortBy, ctx, sctx, scanflg);
    }

    /**
     * Return a pre-computed value of the smlt() for a particular XMLType
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
    public static oracle.ODCI.ODCIRidList TextMlt(XMLType text, String keyStr,
                                       String sortBy, ODCIIndexCtx ctx,
                                                  SolrDomainIndex[] sctx,
                                                  java.math.BigDecimal scanflg) throws SQLException,
                                                                                       IOException,
                                                                                       InvalidTokenOffsetsException {
        return TextMlt(text.getStringVal(), keyStr, sortBy, ctx, sctx,
                       scanflg);
    }

    /**
     * Return a pre-computed value of the smlt() for a particular String
     * text. We assume that OCIFetch function was called first. smlt is
     * know as anciliary operator of scontains, you can get the mlt with:
     * select smlt(1) from tabname where scontains(col,'text to find',1)>0
     * last number argument of lcontains match with the argument of shighlight.
     *
     * @param text
     * @param keyStr
     * @param ctx
     * @param sctx
     * @param scanflg
     * @return
     * @throws SQLException
     */
    public static oracle.ODCI.ODCIRidList TextMlt(String text, String keyStr,
                                       String sortBy, ODCIIndexCtx ctx,
                                                  SolrDomainIndex[] sctx,
                                                  java.math.BigDecimal scanflg) throws SQLException,
                                                                                       IOException,
                                                                                       InvalidTokenOffsetsException {
        logger.trace("TextMlt",
                     new Object[] { text, keyStr, sortBy, ctx, sctx,
                                    scanflg });
        SolrIndexContext sbtctx;
        int key;
        if (scanflg != null) { // Sanity checks
            SQLException t =
                new SQLException("Can not use SMlt() without scontains() in a where side");
            logger.error("TextMlt", t);
            throw t;
        }
        if (sctx == null || sctx[0] == null) { // Sanity checks
            SQLException t =
                new SQLException("SolrDomainIndex parameter is null. Are you using scontains in a not index column?");
            logger.error("TextMlt", t);
            throw t;
        }
        key = sctx[0].getScanctx().intValue();
        // Get the resultSet back from the ContextManager using the key
        sbtctx = (SolrIndexContext)ContextManager.getContext(key);
        if (!sbtctx.isStoreMlt())
            throw new SQLException("TextMlt do not have pre-computed score values");
        Hashtable mlt = sbtctx.getMltListCopy();
        SolrDocumentList docs = (SolrDocumentList)mlt.get(ctx.getRid());
        if (docs == null)
            throw new SQLException("TextMlt can't get a mlt list for rowid= " +
                                   ctx.getRid());
        ArrayList rids = new ArrayList();
        Iterator ite = docs.iterator();
        while (ite.hasNext()) {
            SolrDocument doc = (SolrDocument)ite.next();
            rids.add((String)doc.getFieldValue("rowid"));
        }
        logger.trace("TextMlt", rids);
        return new oracle.ODCI.ODCIRidList((String[])rids.toArray(new String[] { }));
    }

    /**
     * Return a pre-computed value of the sscore() for a particular rowid. We
     * assume that OCIFetch function was called first and store the score for
     * each rowid visited. sscore is know as anciliary operator of lcontains,
     * you can get the score with: select sscore(1) from tabname where
     * lcontains(col,'text to find',1)>0 last number argument of lcontains match
     * with the argument of sscore.
     *
     * @param text
     * @param keyStr
     * @param ctx
     * @param sctx
     * @param scanflg
     * @return
     * @throws SQLException
     */
    public static BigDecimal TextScore(String text, String keyStr,
                                       String sortBy, ODCIIndexCtx ctx,
                                       SolrDomainIndex[] sctx,
                                       java.math.BigDecimal scanflg) throws SQLException,
                                                                            IOException {
        //logger.trace("TextScore" +
        //             new Object[] { text, keyStr, sortBy, ctx, sctx,
        //                            scanflg });
        SolrIndexContext sbtctx;
        int key;
        if (scanflg != null) { // Sanity checks
            SQLException t =
                new SQLException("Can not use sscore() without scontains() in a where side");
            logger.error("TextScore", t);
            throw t;
        }
        if (sctx == null || sctx[0] == null || sctx[0].getScanctx() == null) { // Sanity checks
            SQLException t =
                new SQLException("SolrDomainIndex parameter is null. Are you using scontains in a not index column?");
            logger.error("TextScore", t);
            throw t;
        }
        key = sctx[0].getScanctx().intValue();
        // Get the resultSet back from the ContextManager using the key
        sbtctx = (SolrIndexContext)ContextManager.getContext(key);
        if (!sbtctx.isStoreScore())
            throw new SQLException("TextScore do not have pre-computed score values");
        Hashtable cachedRowids = sbtctx.getScoreListCopy();
        Float score = (Float)cachedRowids.get(ctx.getRid());
        return (score == null) ? null : new BigDecimal(score);
            
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
    public static java.math.BigDecimal ODCIStart(SolrDomainIndex[] sctx,
                                                 ODCIIndexInfo ia,
                                                 ODCIPredInfo op,
                                                 ODCIQueryInfo qi,
                                                 java.math.BigDecimal strt,
                                                 java.math.BigDecimal stop,
                                                 java.math.BigDecimal cmppos,
                                                 String cmpval,
                                                 ODCIEnv env) throws java.sql.SQLException {
        logger.trace("ODCIStart", cmppos);
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
     */
    public static java.math.BigDecimal ODCIStart(SolrDomainIndex[] sctx,
                                                 ODCIIndexInfo ia,
                                                 ODCIPredInfo op,
                                                 ODCIQueryInfo qi,
                                                 java.math.BigDecimal strt,
                                                 java.math.BigDecimal stop,
                                                 String cmpval, String sortval,
                                                 ODCIEnv env) throws java.sql.SQLException {
        if (!op.getObjectName().equalsIgnoreCase("scontains")) {
            SQLException t =
                new SQLException("Expected scontains operator, use scontains(column,'Solr Query'[,'sort string'])>0");
            logger.error("ODCIStart", t);
            throw t;
        }
        String directoryPrefix = getIndexPrefix(ia);
        Parameters par = Parameters.getParameters(directoryPrefix);
        String queryString =  (cmpval == null) ? "" : cmpval.trim();
        String searcherHost = null;
        if (queryString.startsWith("flashback:")) {
            int pos = queryString.indexOf(" AND ");
            if (pos < 0) {
                RuntimeException t =
                    new RuntimeException("Invalid Flashback syntax in scontains() operator, can not find AND conector syntax is 'flashback:nn AND restOfSolrQueryParserSyntax'");
                logger.error("ODCIStart", t);
                throw t;
            }
            String flashbackNumInfo = queryString.substring(10, pos).trim();
            queryString = queryString.substring(pos + 5);
            searcherHost = par.getFlashbackSearcherById(Integer.parseInt(flashbackNumInfo));
        } else
            searcherHost = par.getSolrRandomSearcher();
        String logLevel = par.getParameter("LogLevel", "WARNING");
        Logger julLogger = Logger.getLogger(CLASS_NAME);
        julLogger.setLevel(Level.parse(logLevel));
        if (logger.isTraceEnabled()) {
            logger.trace("ODCIStart",
                         new Object[] { sctx, ia, op, qi, strt, stop, cmpval,
                                        sortval, env });
            OJVMUtil.dumpIA(julLogger, ia);
            OJVMUtil.dumpOP(julLogger, op);
            OJVMUtil.dumpQI(julLogger, qi);
            OJVMUtil.dumpEnv(julLogger, env);
        }
        String sortStr = null;
        int key;
        int qiFlags = qi.getFlags().intValue();
        int numOps = (qi.getAncOps() != null) ? qi.getAncOps().length() : 0;
        int startIndex = 0;
        int endIndex = 0;
        boolean hasPagination = false;
        boolean storeScore = false;
        boolean trackMaxScore = false;
        boolean storeHighlight = false;
        boolean storeMlt = false;
        String highlightCol = null;
        String useFastVectorHighlighter = null;
        String mltCol = null;
        for (int i = 0; i < numOps; i++) {
            String opName = qi.getAncOps().getElement(i).getObjectName();
            //logger.trace("Found op: " + opName);
            if ("SSCORE".equals(opName) || "SFREQTERMS".equals(opName)) {
                storeScore = true;
                trackMaxScore = "true".equalsIgnoreCase(
                                              par.getParameter("NormalizeScore",
                                                               "false"));
            }
            if ("SHIGHLIGHT".equals(opName)) {
                storeHighlight = true;
                highlightCol = par.getParameter("HighlightColumn");
                useFastVectorHighlighter = par.getParameter("UseFastVectorHighlighter", "false");
                if (highlightCol == null)
                    highlightCol = par.getParameter("DefaultColumn");
            }
            if ("SMLT".equals(opName)) {
                storeMlt = true;
                mltCol = par.getParameter("MltColumn");
                if (mltCol == null)
                    mltCol = par.getParameter("DefaultColumn");
            }
        }
        String extraCols = par.getParameter("ExtraCols");
        if (sortval == null || sortval.length() == 0)
            sortStr = getSortStr(qi,extraCols);
        else
            sortStr = sortval;
        
        boolean firstRowHint =
            (qiFlags & QUERY_FIRST_ROWS) == QUERY_FIRST_ROWS;
        if (queryString.startsWith("rownum:[")) {
            int pos = queryString.indexOf(" AND ");
            if (pos < 0) {
                RuntimeException t =
                    new RuntimeException("Invalid rownum syntax in scontains() operator, can not find AND conector syntax is 'rownum:[nn TO mm] AND restOfSolrQueryParserSyntax'");
                logger.error("ODCIStart", t);
                throw t;
            }
            String rowNumInfo = queryString.substring(0, pos);
            queryString = queryString.substring(pos + 5);
            pos = rowNumInfo.indexOf(" TO ");
            if (pos < 0) {
                RuntimeException t =
                    new RuntimeException("Invalid rownum syntax in scontains() operator, no rownum:[nn TO mm] syntax");
                logger.error("ODCIStart", t);
                throw t;
            }
            startIndex =
                    Integer.parseInt(rowNumInfo.substring(8, pos).trim()) - 1;
            endIndex =
                    Integer.parseInt(rowNumInfo.substring(pos + 4, rowNumInfo.length() -
                                                          1).trim()) - 1;
            if (endIndex < 0 || startIndex < 0) { // Sanity checks
                RuntimeException t =
                    new RuntimeException("Invalid rownum syntax in scontains() operator, index can not be less than 1");
                logger.error("ODCIStart", t);
                throw t;
            }
            if (endIndex < startIndex) {
                RuntimeException t =
                    new RuntimeException("Invalid rownum syntax in scontains() operator, end index is less than begin index");
                logger.error("ODCIStart", t);
                throw t;
            }
            hasPagination = true;
        } else if (firstRowHint) {
                 startIndex = 0;
                 endIndex = 999; // Only gets first 1000 hits
               } else {
                 startIndex = 0;
                 endIndex = Integer.MAX_VALUE - 1;
               }
        // inject filter by expresion defined at index creation time
        if (qi.getCompInfo() != null && qi.getCompInfo().getPredInfo() != null) {
            queryString = addFilterByExp(qi.getCompInfo().getPredInfo(), queryString, extraCols);
        }
        if (logger.isInfoEnabled()) {
            logger.info("from: '" + startIndex + "'");
            logger.info("to: '" + endIndex + "'");
            logger.info("queryString: '" + queryString + "'");
        }
        SolrIndexContext sbtctx = new SolrIndexContext();
        String tableSchema = ia.getIndexCols().getElement(0).getTableSchema();
        String tableName = ia.getIndexCols().getElement(0).getTableName();
        sbtctx.setMasterTable(tableSchema + "." + tableName);
        String flStr = (storeScore) ? "rowid score" : "rowid";
        URLConnection u = null;
        InputStream is = null;
        StringBuffer qryStr =
            new StringBuffer("http://" + searcherHost + "/select?wt=javabin&omitHeader=true");
        try {
            qryStr.append("&sort=").append(URLEncoder.encode(sortStr, "UTF-8"));
            qryStr.append("&core=").append(URLEncoder.encode(directoryPrefix, "UTF-8"));
            qryStr.append("&fl=").append(URLEncoder.encode(flStr, "UTF-8"));
            qryStr.append("&q=").append(URLEncoder.encode(queryString, "UTF-8"));
            if (storeHighlight)
                qryStr.append("&hl=true&hl.useFastVectorHighlighter=").append(useFastVectorHighlighter).append("&hl.fl=").append(URLEncoder.encode(highlightCol,
                                                                                                                                                   "UTF-8"));
            if (storeMlt) {
                qryStr.append("&mlt=true&mlt.fl=").append(URLEncoder.encode(mltCol, "UTF-8"));
                qryStr.append("&mlt.mindf=" +
                              par.getParameter("MltMinDf", "1"));
                qryStr.append("&mlt.mintf=" +
                              par.getParameter("MltMinTf", "1"));
                qryStr.append("&mlt.count=" +
                              par.getParameter("MltCount", "5"));
            }
            qryStr.append("&start=").append(startIndex).append("&rows=").append((endIndex -
                                                                                 startIndex +
                                                                                 1));
            //if (logger.isDebugEnabled())
            //    qryStr.append("&debugQuery=true");
            u = new URL(qryStr.toString()).openConnection();
            is = u.getInputStream();
            SimpleOrderedMap m1 =
                (SimpleOrderedMap)new JavaBinCodec().unmarshal(is);
            SolrDocumentList resp = (SolrDocumentList)m1.get("response");
            if (storeHighlight) {
                SimpleOrderedMap highlighting =
                    (SimpleOrderedMap)m1.get("highlighting");
                //logger.trace("highlight value: " + highlighting);
                sbtctx.setHighlightIterator(highlighting.iterator());
                sbtctx.setHighlightCol(highlightCol);
                sbtctx.setHighlightListCopy(new Hashtable(200));
            }
            if (storeMlt) {
                SimpleOrderedMap mlt =
                    (SimpleOrderedMap)m1.get("moreLikeThis");
                //logger.trace("moreLikeThis value: " + mlt);
                sbtctx.setMltIterator(mlt.iterator());
                sbtctx.setMltCol(mltCol);
                sbtctx.setMltListCopy(new Hashtable(200));
            }
            sbtctx.setNumFound(resp.getNumFound());
            if (hasPagination || !firstRowHint) {
                sbtctx.setStartIndex(startIndex);
                if (endIndex > resp.getNumFound())
                    sbtctx.setEndIndex(resp.getNumFound() - 1);
                else
                    sbtctx.setEndIndex(endIndex);
            } else {
                sbtctx.setStartIndex(0);
                sbtctx.setEndIndex(resp.getNumFound() - 1);
            }
            sbtctx.setIterator(resp.iterator());
            sbtctx.setStoreScore(storeScore);
            sbtctx.setStoreHighlight(storeHighlight);
            sbtctx.setStoreMlt(storeMlt);
            if (storeScore) {
                sbtctx.setScoreListCopy(new Hashtable(200));
                if (trackMaxScore)
                    sbtctx.setMaxScore(1.0f / resp.getMaxScore());
                else
                    sbtctx.setMaxScore(1.0f);
            }
            if (logger.isInfoEnabled()) {
                logger.info("Query: " + u.toString());
                logger.info("Num Found: " + resp.getNumFound());
                logger.info("Start: " + resp.getStart());
                logger.info("Doc List size= " + resp.size());
                logger.info("MaxScore= " +resp.getMaxScore());
            }
            key = ContextManager.setContext(sbtctx);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            SQLException sq =
                new SQLException("MalformedURLException doing http request: " +
                                 e.getLocalizedMessage());
            logger.error("ODCIStart", sq);
            throw sq;
        } catch (IOException e) {
            SQLException sq;
            if (u != null) {
                InputStream error = ((HttpURLConnection)u).getErrorStream();
                InputStreamReader ie = new InputStreamReader(error);
                BufferedReader br = new BufferedReader(ie);
                String read = null;
                StringBuffer sb = new StringBuffer();
                try {
                    while ((read = br.readLine()) != null) {
                        sb.append(read);
                    }
                    sq = new SQLException(sb.toString());
                } catch (IOException f) {
                    f.printStackTrace();
                    sq = new SQLException(e);
                }
            } else
                sq = new SQLException(e);
            logger.error("ODCIStart", sq);
            throw sq;
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException ioe) {
                // just going to ignore this one
                ioe.printStackTrace();
            }
        }
        sctx[0] = new SolrDomainIndex();
        // set the key into the self argument so that we can retrieve the
        // context with this key later.
        sctx[0].setScanctx(new Integer(key));
        //logger.trace("ODCIStart", key);
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
    public String ODCIFetch(java.math.BigDecimal nrows, ODCIRidList[] rids,
                            ODCIEnv env) throws java.sql.SQLException {
        //logger.trace("ODCIFetch", new Object[] { nrows, rids, env });
        SolrIndexContext sbtctx; // cntxt obj that holds the ResultSet and
        // Statement
        int key = getScanctx().intValue();
        sbtctx = (SolrIndexContext)ContextManager.getContext(key);
        boolean storeScore = sbtctx.isStoreScore();
        float maxScore = sbtctx.getMaxScore(); // actually this is normalize score
        boolean storeHighlight = sbtctx.isStoreHighlight();
        boolean storeMlt = sbtctx.isStoreMlt();
        Iterator ite = sbtctx.getIterator();
        Iterator<Map.Entry<String, Object>> iteh = sbtctx.getHighlightIterator();
        Iterator<Map.Entry<String, Object>> item = sbtctx.getMltIterator();
        int nRows = nrows.intValue();
        ArrayList<String> rlist = new ArrayList<String>();
        int i = 0;
        Hashtable scoreTable = null;
        Hashtable highlightTable = null;
        Hashtable mltTable = null;
        if (storeScore)
            scoreTable = sbtctx.getScoreListCopy();
        if (storeHighlight)
            highlightTable = sbtctx.getHighlightListCopy();
        if (storeMlt)
            mltTable = sbtctx.getMltListCopy();
        long startIndex = sbtctx.getStartIndex();
        long endIndex = sbtctx.getEndIndex();
        logger.info("beginning window at startIndex: " + startIndex +
                    " endIndex: " + endIndex + " nrows: " + nrows);
        while (ite.hasNext() && startIndex <= endIndex && i < nRows) {
            SolrDocument doc = (SolrDocument)ite.next();
            String rowid = (String)doc.getFieldValue("rowid");
            rlist.add(rowid);
            //logger.trace("rowid: " + rlist[i]);
            if (storeScore)
                scoreTable.put(rowid, (Float)doc.getFieldValue("score") * maxScore);
            if (storeHighlight) {
                Map.Entry<String, Object> entry = iteh.next();
                SimpleOrderedMap val = (SimpleOrderedMap)entry.getValue();
                highlightTable.put(entry.getKey(), val);
            }
            if (storeMlt) {
                Map.Entry<String, Object> entry = item.next();
                Object val = entry.getValue();
                //logger.trace("key: " + key + "val: " + val);
                mltTable.put(entry.getKey(), val);
            }
            i++;
            startIndex++;
        }
        //logger.trace("returned rows: " + rlist.size());
        sbtctx.setStartIndex(startIndex);
        rids[0] = new ODCIRidList(rlist.toArray(new String[] {}));
        //logger.trace("ODCIFetch", sbtctx.getMasterTable());
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
        logger.trace("ODCIClose", env);
        SolrIndexContext sbtctx; // contxt obj that holds the ResultSet and
        // Statement
        int key = getScanctx().intValue();
        sbtctx = (SolrIndexContext)ContextManager.clearContext(key);
        boolean storeScore = sbtctx.isStoreScore();
        boolean storeHighlight = sbtctx.isStoreHighlight();
        boolean storeMlt = sbtctx.isStoreMlt();
        if (storeScore)
            sbtctx.getScoreListCopy().clear();
        if (storeHighlight)
            sbtctx.getHighlightListCopy().clear();
        if (storeMlt)
            sbtctx.getMltListCopy().clear();
        sbtctx = null;
        return SUCCESS;
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
     */
    public static java.math.BigDecimal countHits(String owner,
                                                 String indexName,
                                                 String cmpval) throws SQLException {
        String directoryPrefix = owner + "." + indexName;
        logger.trace("countHits", new Object[] { directoryPrefix, cmpval });
        BigDecimal totalHits;
        URLConnection u = null;
        InputStream is = null;
        Parameters par = Parameters.getParameters(directoryPrefix);
        String queryString = cmpval.trim();
        String searcherHost = null;
        if (queryString.startsWith("flashback:")) {
            int pos = queryString.indexOf(" AND ");
            if (pos < 0) {
                RuntimeException t =
                    new RuntimeException("Invalid Flashback syntax in countHits() function, can not find AND conector syntax is 'flashback:nn AND restOfSolrQueryParserSyntax'");
                logger.error("ODCIStart", t);
                throw t;
            }
            String flashbackNumInfo = queryString.substring(10, pos).trim();
            queryString = queryString.substring(pos + 5);
            searcherHost = par.getFlashbackSearcherById(Integer.parseInt(flashbackNumInfo));
        } else
            searcherHost = par.getSolrRandomSearcher();
        StringBuffer qryStr =
            new StringBuffer("http://" + searcherHost + "/select?wt=javabin&omitHeader=true");
        try {
            qryStr.append("&core=").append(URLEncoder.encode(directoryPrefix, "UTF-8"));
            qryStr.append("&q=").append(URLEncoder.encode(queryString, "UTF-8"));
            qryStr.append("&rows=").append(0);
            //logger.trace("URL to call: " + qryStr.toString());
            u = new URL(qryStr.toString()).openConnection();
            is = u.getInputStream();
            SimpleOrderedMap m1 =
                (SimpleOrderedMap)new JavaBinCodec().unmarshal(is);
            SolrDocumentList resp = (SolrDocumentList)m1.get("response");
            logger.info("Num Found: " + resp.getNumFound());
            totalHits = new BigDecimal(resp.getNumFound());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            SQLException sq =
                new SQLException("MalformedURLException doing http request: " +
                                 e.getLocalizedMessage());
            logger.error("countHits", sq);
            throw sq;
        } catch (IOException e) {
            SQLException sq;
            if (u != null) {
                InputStream error = ((HttpURLConnection)u).getErrorStream();
                InputStreamReader ie = new InputStreamReader(error);
                BufferedReader br = new BufferedReader(ie);
                String read = null;
                StringBuffer sb = new StringBuffer();
                try {
                    while ((read = br.readLine()) != null) {
                        sb.append(read);
                    }
                    sq = new SQLException(sb.toString());
                } catch (IOException f) {
                    f.printStackTrace();
                    sq = new SQLException(e);
                }
            } else
                sq = new SQLException(e);
            logger.error("countHits", sq);
            throw sq;
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException ioe) {
                // just going to ignore this one
                ioe.printStackTrace();
            }
        }
        logger.trace("countHits", totalHits);
        return totalHits;
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
        if (deleted.length() == 0 && inserted.length() == 0) // Sanity checks
            return; // Shorcut ;)
        //logger.trace("sync", new Object[] { prefix, deleted, inserted });
        Parameters par = Parameters.getParameters(prefix);
        String updaterHost = par.getSolrUpdater();
        URL url =
            new URL("http://" + updaterHost + "/update");
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        ArrayList m1 = new ArrayList();
        m1.add(prefix);
        m1.add("sync");
        m1.add(deleted.getArray());
        m1.add(inserted.getArray());
        new JavaBinCodec().marshal(m1, os);
        //write parameters
        os.flush();
        // Get the response
        StringBuffer answer = new StringBuffer();
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            answer.append(line);
        }
        os.close();
        reader.close();
        //logger.trace("exit sync", answer.toString());
    }

    /**
     * Optimize Lucene Index, first Lucene Index parameters from parameters
     * storage then call to IndexWriter.optimize method
     *
     * @param prefix
     * @throws IOException
     * @throws SQLException
     */
    public static void optimize(String directoryPrefix) throws IOException,
                                                               SQLException {
        logger.trace("optimize", directoryPrefix);
        // optimize Lucene Index
        // Latest Lucene impl. deprecated optimize, control this op using 
        // Merge parameters
        IndexWriter writer = null;
        OJVMDirectory dir = null;
        OracleCallableStatement cs = null;
        Parameters par = Parameters.getParameters(directoryPrefix);
        String SyncMode = par.getParameter("SyncMode", "OnLine");
        if ("OnLine".equalsIgnoreCase(SyncMode)) {
            // force to close other index writers, save pending changes
            commit(directoryPrefix,"true");
            logger.info("exit optimize, calling commit with optimize true");
            return;
        }
        try {
            dir = OJVMDirectory.getDirectory(directoryPrefix);
            // before optimize purge deleted documents
            Integer maxSegments = new Integer(par.getParameter("MaxSegments", "1"));
            dir.purge();
            writer = getIndexWriterForDir(dir, false);
            writer.forceMerge(maxSegments);
            writer.commit();
            writer.close();
            writer = null;
            // after optimize purge deleted documents
            dir.purge();
            // after optimize purge deleted documents on slave storage
            dir.close();
            dir = null;
            OJVMUtil.getConnection().commit();
            notifySearchers(directoryPrefix, false);
            notifyUpdater(directoryPrefix);
        } catch (IOException e) {
            logger.error("optimize", e);
            throw e;
        }
        logger.trace("exit optimize");
    }

    /**
   * Commit Solr Index
   *
   * @param prefix
   * @throws IOException
   * @throws SQLException
   */
  public static void commit(String prefix, String optimize) throws IOException,
                                                      SQLException {
        //logger.trace("commit", prefix, optimize);
        Parameters par = Parameters.getParameters(prefix);
        String updaterHost = par.getSolrUpdater();
        boolean waitFlush = "true".equalsIgnoreCase(par.getParameter("WaitFlush", "true"));
        boolean waitSearcher = "true".equalsIgnoreCase(par.getParameter("WaitSearcher", "true"));
        boolean softCommit = "true".equalsIgnoreCase(par.getParameter("SoftCommit", "false"));
        boolean expungeDeletes = "true".equalsIgnoreCase(par.getParameter("ExpungeDeletes", "false"));
        Integer maxSegments = new Integer(par.getParameter("MaxSegments", "1"));
        URL url =
            new URL("http://" + updaterHost + "/update");
        //logger.trace("URL to call: " + url.toString() +
        //                   "passing core: " + prefix);
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        ArrayList m1 = new ArrayList();
        m1.add(prefix);
        m1.add("commit");
        m1.add(waitFlush); // waitFlush // deprecated
        m1.add(waitSearcher); // waitSearcher
        m1.add(softCommit); // softCommit
        m1.add(expungeDeletes); // expungeDeletes
        m1.add(maxSegments); // maxSegments
        m1.add(Boolean.parseBoolean(optimize)); // optimize

        new JavaBinCodec().marshal(m1, os);
        //write parameters
        os.flush();
        // Get the response
        StringBuffer answer = new StringBuffer();
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            answer.append(line);
        }
        os.close();
        reader.close();

        //Output the response
        //logger.trace("exit commit", answer.toString());
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
        logger.trace("rebuild", directoryPrefix);
        try {
            conn = OJVMUtil.getConnection();
            dir = OJVMDirectory.getDirectory(directoryPrefix);
            Parameters parameters = Parameters.getParameters(directoryPrefix);
            // Discard pending changes and purge deleted documents
            discard(directoryPrefix);
            // Index parameters
            String tableSchema = parameters.getParameter("TableSchema");
            String tableName = parameters.getParameter("TableName");
            String partition = parameters.getParameter("Partition");
            String syncMode = parameters.getParameter("SyncMode", "Deferred");
            logger.info("Internal Parameters:\n" +
                    parameters.toString());
            // open a writer object with last argument to true cause truncation
            // on lucene index
            writer = getIndexWriterForDir(dir, true);
            writer.commit();
            writer.close();
            writer = null;
            dir.purge();
            dir.close();
            dir = null;
            conn.commit();
            String allTbl;
            if (partition != null && partition.length() > 0)
              allTbl = tableSchema + "." + tableName + " partition("+partition+")";
            else
                allTbl = tableSchema + "." + tableName;
            if ("OnLine".equalsIgnoreCase(syncMode)) {
                int batchCount =
                    Integer.parseInt(parameters.getParameter("BatchCount",
                                                             "32767"));
                logger.info("BatchCount: " + batchCount);
                cs = (OracleCallableStatement)conn.prepareCall(StringUtils.replace(allRowidsEnqueueStmt,"%TBL%",allTbl));
                cs.setInt(1, batchCount);
                cs.setString(2, directoryPrefix);
                logger.info("Enqueue indexing on table: " + tableSchema + "." +
                            tableName + " partition: " + partition + " index: " + directoryPrefix);
                cs.executeUpdate();
                cs.close();
                cs = null;
            } else {
                conn.commit(); // commit resources to slave process
                // send batch of 32767 rowids for inserting
                int batchCount =
                    Integer.parseInt(parameters.getParameter("BatchCount",
                                                             "32767"));
                logger.info("BatchCount: " + batchCount);
                cs = (OracleCallableStatement)conn.prepareCall(StringUtils.replace(allRowidsTableStmt,"%TBL%",allTbl));
                cs.setInt(1, batchCount);
                cs.setString(2, directoryPrefix);
                logger.info("Start indexing on table: " + tableSchema + "." +
                            tableName + " partition: " + partition + " index: " + directoryPrefix);
                cs.executeUpdate();
                cs.close();
                cs = null;
            }
            logger.trace("rebuild exit", SUCCESS);
        } catch (IOException e) {
            logger.error("rebuild", e);
        } finally {
            try {
                if (writer != null)
                    writer.close();
                writer = null;
                if (dir != null)
                    dir.close();
                dir = null;
            } catch (IOException e) {
                logger.error("rebuild", e);
            }
        }
    }

    public static IndexWriter getIndexWriterForDir(OJVMDirectory dir,
                                                   boolean createEnable) throws SQLException,
                                                                                CorruptIndexException,
                                                                                LockObtainFailedException,
                                                                                IOException {
        IndexWriter writer = null;
        Parameters parameters = dir.getParameters();
        Analyzer analyzer = new SimpleAnalyzer(LUCENE_COMPAT_VERSION);
        IndexWriterConfig ic =
            new IndexWriterConfig(LUCENE_COMPAT_VERSION, analyzer);

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
            ic.setRAMBufferSizeMB(((OracleRuntime.getJavaPoolSize() / 100) *
                                   50) / (1024 * 1024));
        else {
            ic.setRAMBufferSizeMB(OracleRuntime.getJavaPoolSize() / (1024 * 1024));
            ic.setMaxBufferedDocs(maxBufferedDocs);
        }
        ic.setMaxBufferedDeleteTerms(maxBufferedDeleteTerms);
        if (createEnable)
            ic.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        else
            ic.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
        //TODO: use LimitTokenCountAnalyzer instead.
        // int maxFieldLength =
        //    Integer.parseInt(parameters.getParameter("MaxFieldLength",
        //                                             "10000"));
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
        writer = new IndexWriter(dir, ic);
        // TODO: look replacement for infoStream
        //if (logger.isInfoEnabled())
        //    writer.setInfoStream(System.out);
        return writer;
    }

    private static void enableOnLineSync(String prefix,
                                         boolean indexOnRam,
                                         boolean isMaster) {
        OracleCallableStatement cs = null;
        Connection conn = null;
        try {
            conn = OJVMUtil.getConnection();
            // logger.trace("tableName: "+tableName+" columnName: "+columnName+" prefix: "+prefix);
            cs = (OracleCallableStatement)conn.prepareCall(enableCallBackStmt);
            cs.setString(1,
                         prefix + "|" + 0 + ":" + ((indexOnRam) ?
                                                                "R" : "D"));
            cs.execute();
            cs.close();
            cs = null;
            logger.info(".prepareCall '" + enableCallBackStmt + "'");
            logger.info(".setString '" + prefix + "|" + 0 + ":" +
                        ((indexOnRam) ? "R" : "D") + "'");
        } catch (SQLException sqe) {
            logger.error("disableOnLineSync", sqe);
        } finally {
            OJVMUtil.closeDbResources(cs, null);
        }
    }

    private static void disableOnLineSync(String prefix,
                                          boolean indexOnRam,
                                          boolean isMaster) {
        OracleCallableStatement cs = null;
        Connection conn = null;
        try {
            conn = OJVMUtil.getConnection();
            cs = (OracleCallableStatement)conn.prepareCall(disableCallBackStmt);
            cs.setString(1,
                         prefix + "|" + 0 + ":" + ((indexOnRam) ?
                                                                "R" : "D"));
            cs.execute();
            cs.close();
            cs = null;
            logger.info(".prepareCall '" + disableCallBackStmt + "'");
            logger.info(".setString '" + prefix + "|" + 0 + ":" +
                        ((indexOnRam) ? "R" : "D") + "'");
        } catch (SQLException sqe) {
            logger.error("disableOnLineSync", sqe);
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
            logger.error("createLuceneStore", sqe);
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
            logger.error("dropLuceneStore", sqe);
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
            logger.error("dropLuceneStore", sqe);
        } finally {
            OJVMUtil.closeDbResources(cs, null);
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
        logger.trace("discard", prefix);
        try {
            conn = OJVMUtil.getConnection();
            cs = (OracleCallableStatement)conn.prepareCall(purgueLuceneQueueStmt);
            cs.setString(1, prefix);
            cs.execute();
            logger.info(".prepareCall :" + purgueLuceneQueueStmt);
            logger.info(".setString :" + prefix);
        } catch (SQLException e) {
            logger.error(".discard : " + e.getLocalizedMessage());
        } finally {
            OJVMUtil.closeDbResources(cs, null);
        }
    }

    /**
     * notify to all parallel readers that this core should be unloaded
     * @param prefix
     */
    public static void notifySearchers(String prefix, boolean unloadCore) {
        Parameters par = Parameters.getParameters(prefix);
        String hostIDs = par.getParameter("Searcher", "0");
        String hostUpdater = par.getSolrUpdater();
        String[] rmtSearcherHostIDs = hostIDs.split(",");
        for (int i = 0; i < rmtSearcherHostIDs.length; i++) {
            String hostPort;
            try {
                int id = Integer.parseInt(rmtSearcherHostIDs[i]);
                hostPort = par.getSolrServerById(id);
            } catch (NumberFormatException e) {
                hostPort = par.getSolrServerById(0);
            }
            if (hostPort.equalsIgnoreCase(hostUpdater))
                continue; // If Updater/Searcher is the same do not notify
            StringBuffer qryStr =
                new StringBuffer("http://" + hostPort + "/select?wt=javabin&omitHeader=true&core.action=" +
                                 (unloadCore ? "delete" : "reload") +
                                 "&core=" + prefix);
            URL u;
            try {
                u = new URL(qryStr.toString());
                logger.info("Contacting replica: " + u.toString());
                InputStream is = u.openStream();
                // Get the response
                StringBuffer answer = new StringBuffer();
                BufferedReader reader =
                    new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = reader.readLine()) != null) {
                    answer.append(line);
                }
                is.close();
                reader.close();
            } catch (MalformedURLException e) {
                logger.error("notifySearchers", e);
            } catch (IOException e) {
                logger.error("notifySearchers", e);
            }
        }
    }

    /**
     * notify to all parallel readers that this core should be unloaded
     * @param prefix
     */
    public static void notifyUpdater(String prefix) {
        Parameters par = Parameters.getParameters(prefix);
        String updaterHost = par.getSolrUpdater();
        StringBuffer qryStr =
            new StringBuffer("http://" + updaterHost + "/select?wt=javabin&omitHeader=true&core.action=delete" +
                             "&core=" + prefix);
        URL u;
        try {
            u = new URL(qryStr.toString());
            logger.info("Contacting replica: " + u.toString());
            InputStream is = u.openStream();
            // Get the response
            StringBuffer answer = new StringBuffer();
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                answer.append(line);
            }
            is.close();
            reader.close();
        } catch (MalformedURLException e) {
            logger.error("notifyUpdater", e);
        } catch (IOException e) {
            logger.error("notifyUpdater", e);
        }
    }

    /**
     * Change default Log Level value
     *
     * @param logLevel
     * @see Level#parse for more detail of logLevel value
     */
    public static void setLogLevel(String logLevel) {
        Logger julLogger = Logger.getLogger(CLASS_NAME);
        julLogger.setLevel(Level.parse(logLevel));
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
        String extraColsArr[] = getExtraColsArr(extraCols);
        for (int i=1;i<cols.length;i++) { // Skip first col, master column of the index
            String colName = cols[i].getColName().replaceAll("\"", "");
            int j=0;
            while(j<extraColsArr.length) {
                int pos = extraColsArr[j].lastIndexOf(' ');
                String col = extraColsArr[j].substring(0, pos).trim().replaceAll("\"", "");
                if (colName.equalsIgnoreCase(col))
                    break; // found
                j++;
            }
            if (j == extraColsArr.length)
                str = str + "," + colName + " \"" + colName + "\"";
        }
        if (str.startsWith(","))
            str = str.substring(1);
        logger.info("new ExtraCols: " + str);
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
            String extraColsArr[] = getExtraColsArr(extraCols);
            int j=0;
            while(j<extraColsArr.length) {
                int pos = extraColsArr[j].lastIndexOf(' ');
                String colStr = extraColsArr[j].substring(0, pos).trim().replaceAll("\"", "");
                if (colName.equalsIgnoreCase(colStr)) {
                    aliasName = extraColsArr[j].substring(pos+1).trim().replaceAll("\"", "");
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
                // AND alias:"val"
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
        //System.out.println("extraCols: " + extraCols);
        if (compQry != null && compQry.getObyInfo() != null && compQry.getObyInfo().length() > 0) {
            sortStr = "";
            ODCIOrderByInfoList obyLst = compQry.getObyInfo();
            ODCIOrderByInfo arr[] = obyLst.getArray();
            String extraColsArr[] = getExtraColsArr(extraCols);
            for (int i=0;i<arr.length;i++) {
                ODCIOrderByInfo obyInfo = arr[i];
                if (obyInfo.getExprType().intValue() == 2) {
                    // ExprType == 2 sscore operator
                    if (obyInfo.getSortOrder().intValue() == 1)
                        sortStr = sortStr + ",score asc";
                    else
                        sortStr = sortStr + ",score desc";
                } else
                    for (int j=0;j<extraColsArr.length;j++) {
                        int pos = extraColsArr[j].lastIndexOf(' ');
                        String col = extraColsArr[j].substring(0, pos).trim().replaceAll("\"", "");
                        String alias = extraColsArr[j].substring(pos+1).trim().replaceAll("\"", "");
                        //System.out.println("col: " + col + " alias: " + alias);
                        if (col.equalsIgnoreCase(obyInfo.getExprName())) {
                            if (obyInfo.getSortOrder().intValue() == 1)
                                sortStr = sortStr + "," + alias + " asc";
                            else
                                sortStr = sortStr + "," + alias + " desc";
                        }
                    }
            }
            sortStr = sortStr.substring(1);
        } else
            sortStr =
                  (((qiFlags & QUERY_SORT_ASC) == QUERY_SORT_ASC) ? "score asc" :
                   "score desc"); // no scontains(col,qry,sort) option, use ODCI flags
        logger.info("Computed sort string: " + sortStr);
        return sortStr;
    }
    
    private static String [] getExtraColsArr(String extraCols) {
        Matcher matcher = patternColAliasList.matcher(extraCols);
        ArrayList <String>arrCols = new ArrayList<String>();
        int i = 0;
        while (matcher.find()) {
              String groupStr = matcher.group();
              if (groupStr.startsWith(","))
                  arrCols.add(groupStr.substring(1).trim());
              else
                  arrCols.add(groupStr.trim());
              i++;
        }
        //System.out.println(arrCols);
        return arrCols.toArray(new String[]{});
    }
}
