package com.scotas.solr.handler;


import com.scotas.lucene.indexer.Parameters;
import com.scotas.lucene.store.OJVMUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.math.BigDecimal;

import java.math.BigInteger;

import java.net.URL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import java.util.Calendar;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import oracle.ODCI.ODCIRidList;

import oracle.jdbc.OracleCallableStatement;

import oracle.sql.ARRAY;
import oracle.sql.CLOB;

import oracle.sql.TIMESTAMP;

import oracle.sql.TIMESTAMPLTZ;
import oracle.sql.TIMESTAMPTZ;

import oracle.xdb.XMLType;

import javax.xml.parsers.SAXParser;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.handler.ContentStreamLoader;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.CommitUpdateCommand;
import org.apache.solr.update.DeleteUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class RowIDLoader extends ContentStreamLoader {
    static final Logger log = LoggerFactory.getLogger(RowIDLoader.class);

    protected SAXParser saxParser;
    protected DefaultHandler saxHandler;

    static final String enqueueChangeStmt =
        "declare\n" +
        "      enqueue_options     DBMS_AQ.enqueue_options_t;\n" + 
        "      message_properties  DBMS_AQ.message_properties_t;\n" + 
        "      message_handle      RAW(16);\n" + 
        "      message             LUCENE.lucene_error_typ;\n" + 
        "begin\n" + 
        "      message := LUCENE.lucene_error_typ(LUCENE.lucene_msg_typ(?,?),?);\n" + 
        "      enqueue_options.visibility         := dbms_aq.immediate;\n" + 
        "      dbms_aq.enqueue(queue_name         => 'LUCENE.ERRORS$Q',\n" + 
        "                      enqueue_options    => enqueue_options,\n" + 
        "                      message_properties => message_properties,\n" + 
        "                      payload            => message,\n" + 
        "                      msgid              => message_handle);\n" + 
        "end;\n";

    /**
     * Database connection used by this indexer
     */
    protected Connection conn;

    /**
     * Schema (database user name) owner of the table to index
     */
    protected String colName;

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

    protected boolean includeMasterColumn;

    /**
     * If CommitOnSync parameter is enabled
     */
    protected boolean commitOnSync;

    protected boolean softCommit;

    protected boolean waitSearcher;

    protected boolean expungeDeletes;

    protected int maxSegments;

    protected UpdateRequestProcessor processor;
    protected String prefix;
    String[] deleted;
    String[] inserted;

    public RowIDLoader(UpdateRequestProcessor processor, String prefix,
                       String[] deleted,
                       String[] inserted) throws SQLException {
        super();
        this.processor = processor;
        this.prefix = prefix;
        this.deleted = deleted;
        this.inserted = inserted;
        Parameters par = Parameters.getParameters(prefix);
        this.extraColsStr = par.getParameter("ExtraCols");
        this.extraTabsStr = par.getParameter("ExtraTabs");
        this.extraWhereStr = par.getParameter("WhereCondition");
        this.lockMasterTable =
                "true".equalsIgnoreCase(par.getParameter("LockMasterTable",
                                                         "false"));
        this.commitOnSync =
                "true".equalsIgnoreCase(par.getParameter("CommitOnSync",
                                                         "false"));
        this.softCommit =
                "true".equalsIgnoreCase(par.getParameter("SoftCommit", "false"));
        this.waitSearcher =
                "true".equalsIgnoreCase(par.getParameter("WaitSearcher",
                                                         "true"));
        this.expungeDeletes =
                "true".equalsIgnoreCase(par.getParameter("ExpungeDeletes",
                                                         "false"));
        this.maxSegments =
                Integer.parseInt(par.getParameter("MaxSegments", "1"));
        this.schemaName = par.getParameter("TableSchema");
        this.tableName = par.getParameter("TableName");
        this.colName = par.getParameter("ColName");
        this.partitionName = par.getParameter("Partition");
        this.includeMasterColumn =
                "true".equalsIgnoreCase(par.getParameter("IncludeMasterColumn",
                                                         "true"));
        this.conn = OJVMUtil.getConnection();
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            saxParser = factory.newSAXParser();
            saxHandler = new MyContentHandler();
        } catch (SAXException e) {
            log.error("factory.newSAXParser()",e);
        } catch (ParserConfigurationException e) {
            log.error("factory.newSAXParser()",e);
        }
        if (log.isDebugEnabled())
            log.debug("RowIDLoader is using connection: " + this.conn);
    }

    public void load(SolrQueryRequest req, SolrQueryResponse rsp,
                     ContentStream stream) throws Exception {
    }

    public void processSync(SolrQueryRequest req,
                            UpdateRequestProcessor processor) throws IOException {
        OracleCallableStatement cs = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        if (deleted.length > 0) {
            log.info("Starting deletions: " + deleted.length);
            DeleteUpdateCommand delCmd = new DeleteUpdateCommand(req);
            try {
                cs = (OracleCallableStatement)conn.prepareCall(enqueueChangeStmt);
                StringBuffer deleteQry = new StringBuffer("rowid:(");
                for (int i = 0; i < deleted.length; i++)
                    deleteQry.append('"').append(deleted[i]).append("\" OR ");
                // process deletions
                try {
                    String boolStr = deleteQry.toString();
                    delCmd.query =
                            boolStr.substring(0, boolStr.length() - 4) + ")";
                    processor.processDelete(delCmd);
                    if (log.isTraceEnabled())
                        log.trace("Deleting: " + delCmd);
                } catch (Exception e) {
                    log.error("failed to delete: " + delCmd, e);
                } finally {
                    delCmd.id = null;
                    delCmd.query = null;
                }
            } catch (SQLException e) {
                log.error("failed to delete rowids: " + deleted, e);
            } finally {
                OJVMUtil.closeDbResources(cs, null);
                cs = null;
            }
        }
        if (inserted.length > 0) {
            log.info("Starting insertions: " + inserted.length);
            String sqlStr =
            getSelectStmt(this.colName,
                            this.includeMasterColumn,
                            this.lockMasterTable, this.extraColsStr,
                            this.extraTabsStr, this.extraWhereStr,
                            this.schemaName, this.tableName,
                            this.partitionName).replace("L$PT, 10",
                                                        "L$PT, " +
                                                        inserted.length);
            if (log.isDebugEnabled())
                log.debug("SelectStmt: " + sqlStr);
            long startTime = System.currentTimeMillis();
            long dbScanTime = 0;
            ODCIRidList rids = new ODCIRidList(inserted);
            try {
                cs = (OracleCallableStatement)conn.prepareCall(enqueueChangeStmt);
                stmt = this.conn.prepareStatement(sqlStr);
                stmt.setObject(1, rids);
                rs = stmt.executeQuery();
                ResultSetMetaData mtdt = rs.getMetaData();
                int numExtraCols =
                    mtdt.getColumnCount() - ((includeMasterColumn) ? 2 : 1);
                int offset = (includeMasterColumn) ? 3 : 2;
                String[] extraCols = new String[numExtraCols];
                for (int i = 0; i < numExtraCols; i++)
                    extraCols[i] = mtdt.getColumnName(i + offset);
                dbScanTime += (System.currentTimeMillis()-startTime);
                AddUpdateCommand addCmd = new AddUpdateCommand(req);
                startTime = System.currentTimeMillis();
                while (rs.next()) {
                    addCmd.clear();
                    addCmd.solrDoc = new SolrInputDocument();
                    String rowid = rs.getString(1);
                    addCmd.solrDoc.addField("rowid", rowid);
                    if (includeMasterColumn) {
                        Object o = rs.getObject(2);
                        readColumn(addCmd.solrDoc,this.colName,o);
                    }
                    for (int i = 0; i < numExtraCols; i++) {
                        Object o = rs.getObject(i + offset);
                        readColumn(addCmd.solrDoc,extraCols[i],o);
                    }
                    if (log.isTraceEnabled())
                      log.trace("Adding: " + addCmd.solrDoc.toString());
                    try {
                        processor.processAdd(addCmd);
                    } catch (Exception e) {
                        try {
                            cs.setObject(1,
                                         new ODCIRidList(new String[] { rowid }));
                            cs.setString(2, "insert");
                            cs.setString(3, this.prefix);
                            cs.execute();
                        } catch (SQLException s) {
                            log.error("failed to enqueue exception when inserting rowid: " +
                                               rowid,s);
                        }
                        log.error("failed to insert rowid: " + rowid, e);
                    }
                }
                dbScanTime += (System.currentTimeMillis()-startTime);
            } catch (SQLException e) {
                try {
                    cs.setObject(1, rids);
                    cs.setString(2, "insert");
                    cs.setString(3, this.prefix);
                    cs.execute();
                } catch (SQLException s) {
                    log.error("failed to enqueue exception when inserting rowid: " +
                                       rids,s);
                }
                log.error("Error processing inserts",e);
                log.error("when excecuting:\n" + sqlStr);
            } finally {
                OJVMUtil.closeDbResources(stmt, rs);
                OJVMUtil.closeDbResources(cs, null);
            }
            if (log.isInfoEnabled())
                log.info("DB Scan elapsed time: " + dbScanTime);
        }
        if (commitOnSync) { // add commit cmd
            CommitUpdateCommand commitCmd =
                new CommitUpdateCommand(req, false);
            //TODO: Deprecated??
            //commitCmd.waitFlush = this.waitFlush;
            commitCmd.softCommit = this.softCommit;
            commitCmd.waitSearcher = this.waitSearcher;
            commitCmd.expungeDeletes = this.expungeDeletes;
            commitCmd.maxOptimizeSegments = this.maxSegments;
            try {
                log.info("Commit changes");
                processor.processCommit(commitCmd);
            } catch (Exception e) {
                log.error("Failed to commit transaction",e);
            }
        }
        log.info("End processSync");
    }

    /**
     * Returns a select stamenent like:
     * select T1.F2,t1.f3,t1.f4,t2.f6,t2.f7 from T1,t2 where t1.f4=t2.f5;
     * if ExtraTabs is t2 and ExtraCols is t1.f3,t1.f4,t2.f6,t2.f7
     * Note that: T1.F2 is the master table.columns of the index
     * @param col Master column of the index
     * @param withMasterColumn if true includes master column in select
     * @return a JDBC Select statement
     */
    public static String getSelectStmt(String col, 
                                 boolean withMasterColumn, 
                                 boolean lockMasterTable, 
                                 String extraColsStr, 
                                 String extraTabsStr, 
                                 String extraWhereStr, 
                                 String schemaName, 
                                 String tableName, 
                                 String partitionName) {
        if (!withMasterColumn && extraColsStr == null) // Sanity checks
            throw new RuntimeException("Can't use flag IncludeMasterColumn:false without ExtraCols parameter");
        StringBuffer selectStmt = new StringBuffer("SELECT ");
        selectStmt.append("/*+ DYNAMIC_SAMPLING(L$MT 0) */ ");
        selectStmt.append("L$MT").append(".rowid");
        if (withMasterColumn)
            selectStmt.append(",").append("L$MT").append(".\"").append(col).append("\"");
        if (extraColsStr != null && extraColsStr.length() > 0)
            selectStmt.append(",").append(extraColsStr);
        if (partitionName != null && partitionName.length() > 0)
            selectStmt.append(" FROM ").append(schemaName).append(".").append(tableName).append(" PARTITION (").append(partitionName).append(") L$MT");
        else // no partitioning information
            selectStmt.append(" FROM ").append(schemaName).append(".").append(tableName).append(" L$MT");
        if (extraTabsStr != null && extraTabsStr.length() > 0)
            selectStmt.append(",").append(extraTabsStr);
        selectStmt.append(" where ");
        selectStmt.append("L$MT").append(".rowid in (select /*+ cardinality(L$PT, 10) */ * from table(?) L$PT) ");
        if (extraWhereStr != null && extraWhereStr.length() > 0)
            selectStmt.append("and (").append(extraWhereStr).append(")");
        if (lockMasterTable)
            selectStmt.append(" for update nowait");
        //System.out.println("select stmt: " + selectStmt);
        return selectStmt.toString();
    }

    public static String readStream(BufferedReader reader) {
        StringBuffer buffer = new StringBuffer();
        try {
            while (true) {
                String line = reader.readLine();
                if (line != null) {
                    buffer.append(line);
                } else {
                    break;
                }
            }
        } catch (IOException iox) {
            iox.printStackTrace();
        }
        return buffer.toString();
    }

    /**
     * @param obj
     * @return an string representation of the XMLType without tags
     * @throws IOException
     * @throws SQLException
     */
    public StringBuffer textExtractor(XMLType obj) throws IOException,
                                                          SQLException {
        StringBuffer result = new StringBuffer("");
        if (obj == null) // Sanity checks
            return result;
        ((MyContentHandler)saxHandler).setTextResult(result);
        try {
            saxParser.parse(obj.getInputStream(),saxHandler);
        } catch (SAXException e) {
            e.printStackTrace();
            throw new SQLException("SAXException: " + e.getMessage());
        }
        //System.out.println("saxHandler: " + saxHandler + " result: " + result);
        ((MyContentHandler)saxHandler).setTextResult(null);
        return result;
    }

    /**
     * @param solrDoc the add command created for this batch of rows
     * @param fldName the field name
     * @param o the column value to be processes
     * @throws SQLException
     * @throws IOException
     */
    public void readColumn(SolrInputDocument solrDoc, String fldName,
                           Object o) throws SQLException, IOException {
        if (o == null)
            return;
        //System.out.println(".readColumn: " + fldName + " class: " + o.getClass());
        if (o instanceof CLOB)
            solrDoc.addField(fldName,
                                    readStream(new BufferedReader(((CLOB)o).characterStreamValue())));
        else if (o instanceof BigDecimal || o instanceof BigInteger) 
            // convert to string if not JavaBinCodec fail to serialize this doc
            solrDoc.addField(fldName,
                                    o.toString());
        else if (o instanceof XMLType)
            solrDoc.addField(fldName,
                                    textExtractor((XMLType)o).toString());
        else if (o instanceof URL)
            solrDoc.addField(fldName,
                                    readStream(new BufferedReader(new InputStreamReader(((URL)o).openStream()))));
        else if (o instanceof ResultSet)
            readResultSet(solrDoc, fldName, (ResultSet)o);
        else if (o instanceof TIMESTAMP)
            solrDoc.addField(fldName, ((TIMESTAMP)o).timestampValue());
        else if (o instanceof TIMESTAMPTZ)
            solrDoc.addField(fldName,
                                    ((TIMESTAMPTZ)o).timestampValue(this.conn));
        else if (o instanceof TIMESTAMPLTZ)
            solrDoc.addField(fldName,
                                    ((TIMESTAMPLTZ)o).timestampValue(this.conn,
                                                                     Calendar.getInstance()));
        else if (o instanceof ARRAY)
            // get Array elements
            parseArray(solrDoc, fldName, (Object[])((ARRAY)o).getArray());
        else { // any other should work as Object.toString
            //log.info("Adding using toString: " + o.getClass().toString());
            solrDoc.addField(fldName, o);
        }
    }
    
    /**
     * If a column is an inner cursor 
     * @param rs 
     * transform it to a multivalued fields using 
     * @param fldName as base name for plus an underscore and the column name
     * @param solrDoc the add command created for this batch of rows
     * @throws SQLException
     * @throws IOException
     */
    public void readResultSet(SolrInputDocument solrDoc, String fldName,
                              ResultSet rs) throws SQLException, IOException {
        ResultSetMetaData mtdt = rs.getMetaData();
        int numExtraCols = mtdt.getColumnCount();
        String[] extraCols = new String[numExtraCols];
        for (int i = 0; i < numExtraCols; i++)
            extraCols[i] = mtdt.getColumnName(i+1);
        while (rs.next())
            for (int i = 0; i < numExtraCols; i++) {
                Object o = rs.getObject(i + 1);
                readColumn(solrDoc,fldName + "_" + extraCols[i],o);
            }
        rs.close();
    }

    /**
     * Transfor a column of type VARRAY or PLSQL Table to multivalued field
     * @param solrDoc, the add command created for this batch of rows
     * @param fldName the field name, it should be with multivalued=true
     * @param values array of values included into the VARRAR/TABLE OF column
     * @throws SQLException
     * @throws IOException
     */
    public void parseArray(SolrInputDocument solrDoc, String fldName, Object[] values) throws SQLException, IOException {
        for (int j=0; j<values.length; j++) {
            Object o = values[j];
            readColumn(solrDoc,fldName,o);
        }
    }
    
    public static class MyContentHandler extends DefaultHandler {
        StringBuffer textResult = null;

        public void startElement(String uri, String localName, String qName,
                                 Attributes atts) {
            //System.out.println("Start Element :" + qName);
            int i = atts.getLength();
            for (int j = 0; j < i; j++) {
                textResult.append(atts.getValue(j)).append(' ');
                //System.out.println(".adding: "+atts.getValue(j));
            }
        }

        public void endElement(String uri, String localName, String qName) {
            //System.out.println("End Element :" + qName);
        }

        public void characters(char[] chars, int start, int length) {
            String strPart = new String(chars, start, length);
            textResult.append(strPart).append(' ');
            //System.out.println(".adding: "+strPart);
        }

        public void setTextResult(StringBuffer buff) {
            this.textResult = buff;
        }
    }
}
