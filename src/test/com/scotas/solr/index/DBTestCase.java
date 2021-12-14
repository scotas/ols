package com.scotas.solr.index;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.sql.Types;

import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

import oracle.jdbc.pool.OracleDataSource;


/**
 * This is base class for most of the test suites includes.
 * It provides a connection pool using OracleDataSource with a
 * minimum of two ready to use connection and growing to 5,
 * after this it will wait up to 20 seconds for free connection.
 * This connection pool is created at the class constructor.
 * Utility methods provided by this class use is own SQLConnection,
 * so they are autonomous transactions
 */
public class DBTestCase extends TestCase {
    private static Logger logger = null;

    /**
     * Constant used to get Logger name
     */
    static final String CLASS_NAME = DBTestCase.class.getName();

    static String TABLE = "T1";
    static String LINDEX = "SIT1"; // Requires LUCENEUSER ROLE
    static String OINDEX = "OIT1"; // Requires CTXAPP ROLE
    static String LOG_LEVEL = "WARNING";
    static String EXTRA_PARAMETERS = "Updater:"+System.getProperty("idx.upd", "0")+
                                     ";Searcher:"+System.getProperty("idx.sch","0,1")+
                                     ";SoftCommit:true;IncludeMasterColumn:false;ExtraCols:F2 \"text\",F1 \"f1_ti\";LobStorageParameters:STORAGE (BUFFER_POOL KEEP) RETENTION AUTO ENABLE STORAGE IN ROW CHUNK 32768 CACHE READS";
    static boolean luceneMode = true;

    static {
        logger = Logger.getLogger(CLASS_NAME);
        logger.setLevel(Level.parse(LOG_LEVEL));
    }

    OracleDataSource              ods = null;
    Connection                   conn = null;
    PreparedStatement insertStatement = null;
    PreparedStatement deleteStatement = null;
    PreparedStatement updateStatement = null;
    PreparedStatement   findStatement = null;
    long startTime = 0;

    /**
     * Create a connection pool.
     * @throws SQLException
     */
    public DBTestCase() throws SQLException {
        ods = new OracleDataSource();
        java.util.Properties prop = new java.util.Properties();
        prop.setProperty("MinLimit", "2");
        prop.setProperty("MaxLimit", "20");
        prop.setProperty("ConnectionWaitTimeout", "20");
        // set DataSource properties
        //String url = "jdbc:oracle:oci8:@CSDS1";
        //            String url = "jdbc:oracle:thin:@mppdbx01.generali.it:1622:CSDK1";
        //            String url = "jdbc:oracle:thin:@mppdbx01.generali.it:1626:CSDS1";
        //            String url = "jdbc:oracle:thin:@(DESCRIPTION =(ADDRESS_LIST =(ADDRESS = (PROTOCOL = TCP)(HOST = mppdbx01.generali.it)(PORT = 1626)) ) (CONNECT_DATA = (SERVER = DEDICATED) (SID = CSDS1) ) )";
        String url =
            "jdbc:oracle:oci8:@" + System.getProperty("db.str", "orcl");
        ods.setURL(url);
        ods.setUser(System.getProperty("db.usr", "lucene"));
        ods.setPassword(System.getProperty("db.pwd", "lucene"));
        ods.setExplicitCachingEnabled(true); // be sure set to true
        ods.setConnectionProperties(prop);
    }

    /**
     * create a test table
     * @throws SQLException if there are errors
     */
    public void createTable() throws SQLException {
        Statement myStatement = null;
        try {
            myStatement = conn.createStatement();
            myStatement.executeUpdate("create table " + TABLE + " (\n" +
                    "f1 number primary key,\n" +
                    "f2 varchar2(200),\n" +
                    "f3 varchar2(200),\n" +
                    "f4 number)");
            System.out.println("Table created: " + TABLE);
        } catch (SQLException s) {
            System.err.println("Error during create Table: " +
                               s.getLocalizedMessage());
            s.printStackTrace();
        } finally {
            if (myStatement != null)
                myStatement.close();
            myStatement = null;
        }
    }

    /**
     * drop table created above
     * @throws SQLException if there are errors
     */
    public void dropTable() throws SQLException {
        Statement myStatement = null;
        try {
            myStatement = conn.createStatement();
            myStatement.executeUpdate("drop table t1");
            System.out.println("Table droped: " + TABLE);
        } catch (SQLException s) {
            System.err.println("Error during drop Table: " +
                               s.getLocalizedMessage());
            s.printStackTrace();
        } finally {
            if (myStatement != null)
                myStatement.close();
            myStatement = null;
        }
    }

    /**
     * add a Lucene Domain Index to previous one created table as follow, with SyncMode:Deferred
     * (LogLevel,MergeFactor,ExtraCols and FormatCols are customizable at class level,
     * after index creation MergeFactor is reduced to 2
     * @throws SQLException if there are errors
     */
    public void createIndex() throws SQLException {
        createIndex(false,true);
    }

    /**
     * add a Lucene Domain Index to previous one created table as follow,
     * (LogLevel,MergeFactor,ExtraCols and FormatCols are customizable at class level,
     * after index creation MergeFactor is reduced to 2
     * @throws SQLException if there are errors
     */
    public void createIndex(boolean onLine, boolean commitOnSync) throws SQLException {
        Statement myStatement = null;
        try {
            myStatement = conn.createStatement();
            if (luceneMode) {
                myStatement.executeUpdate("create index " + LINDEX + " on " +
                                          TABLE +
                                          "(f2) indextype is lucene.SolrIndex filter by f1 order by f1 parameters('LockMasterTable:false;LogLevel:" +
                                          LOG_LEVEL + ";" +
                                          EXTRA_PARAMETERS + "')");
                myStatement.executeUpdate("alter index " + LINDEX +
                                          " parameters('CommitOnSync:" + commitOnSync + ";" +
                                          ((onLine) ? "SyncMode:OnLine" :
                                           "SyncMode:Deferred") + "')");
                System.out.println("Index altered: " + LINDEX);
            } else {
                myStatement.executeUpdate("create index " + OINDEX + " on " +
                                          TABLE +
                                          "(f2) indextype is ctxsys.context filter by f1 order by f1 parameters('SYNC (" +
                                          ((onLine) ? "ON COMMIT" : "MANUAL") +
                                          ")')");
                System.out.println("Index created: " + OINDEX);
            }
        } catch (SQLException s) {
            System.err.println("Error during create Index: " +
                               s.getLocalizedMessage());
            s.printStackTrace();
        } finally {
            if (myStatement != null)
                myStatement.close();
            myStatement = null;
        }
    }

    /**
     * drop previous one index
     * @throws SQLException
     */
    public void dropIndex() throws SQLException {
        Statement myStatement = null;
        try {
            myStatement = conn.createStatement();
            if (luceneMode) {
                myStatement.executeUpdate("drop index " + LINDEX);
                System.out.println("Index droped: " + LINDEX);
            } else {
                myStatement.executeUpdate("drop index " + OINDEX);
                System.out.println("Index droped: " + OINDEX);
            }
        } catch (SQLException s) {
            System.err.println("Error during drop Index: " +
                               s.getLocalizedMessage());
            s.printStackTrace();
        } finally {
            if (myStatement != null)
                myStatement.close();
            myStatement = null;
        }
    }

    /**
     * insert a set of rows at above table
     * F2 column is an english text representation of F1,
     * F4 is F1*10 and
     * F3 is an english text representation of F1*10
     * @param startIndex F1 starting value
     * @param endIndex F1 end value
     * @return number of rows inserted
     * @throws SQLException If there are problems such as primary key violation it rollback the transaction
     */
    public long insertRows(int startIndex, int endIndex) throws SQLException {
        int rowCount = 0;
        long avgTime = 0;
        try {
            long elapsedTime = 0;
            insertStatement.setInt(1, startIndex);
            insertStatement.setInt(2, startIndex);
            insertStatement.setInt(3, startIndex);
            insertStatement.setInt(4, startIndex);
            insertStatement.setInt(5, endIndex - startIndex + 1);
            elapsedTime = System.currentTimeMillis();
            rowCount = insertStatement.executeUpdate();
            //conn.commit(); // OText working in OnLine mode will apply changes here
            elapsedTime = System.currentTimeMillis() - elapsedTime;
            avgTime = ((rowCount > 0) ? elapsedTime / rowCount : 0);
            //System.out.println("Inserted rows: " + rowCount + " time: "+ elapsedTime + " avg time: " + avgTime);
        } catch (SQLException s) {
            if(s.getErrorCode()!=1)
              s.printStackTrace();
            //conn.rollback();
        }
        return avgTime;
    }

    /**
     * delete a set of rows where F1 between startIndex and endIndex
     * @param startIndex F1 starting value
     * @param endIndex F1 end value
     * @return a number of rows deleted
     * @throws SQLException If there are problems rollback the transaction
     */
    public long deleteRows(int startIndex, int count) throws SQLException {
        int rowCount = 0;
        long elapsedTime = 0;
        try {
            deleteStatement.setInt(1, startIndex);
            deleteStatement.setInt(2, startIndex + count);
            elapsedTime = System.currentTimeMillis();
            rowCount = deleteStatement.executeUpdate();
            //conn.commit();
            if (rowCount > 0) { }
            /*    System.out.println("Row deleted " + rowCount + ", from: " +
                                   startIndex +
                                   " elapsed time: " +
                                   (System.currentTimeMillis() - elapsedTime));
            else
                System.out.println("No Row deleted at: " + startIndex +
                                   " to: " + count +
                                   " elapsed time: " +
                                   (System.currentTimeMillis() - elapsedTime)); */
        } catch (SQLException s) {
            System.out.println("Exception when deleting rows: " + startIndex +
                               " to: " + count + " - " +
                               s.getLocalizedMessage());
            //conn.rollback();
        }
        elapsedTime = System.currentTimeMillis() - elapsedTime;
        return (rowCount > 0) ? elapsedTime / rowCount : 0;
    }

    /**
     * update F2 column with his own value to fire ODCI update method
     * on each row between startIndex and endIndex
     * @param startIndex F1 starting value
     * @param endIndex F1 end value
     * @return a number of rows updated
     * @throws SQLException If there are problems rollback the transaction
     */
    public long updateRows(int startIndex, int count) throws SQLException {
        int rowCount = 0;
        long elapsedTime = 0;
        try {
            updateStatement.setInt(1, startIndex);
            updateStatement.setInt(2, startIndex + count);
            elapsedTime = System.currentTimeMillis();
            rowCount = updateStatement.executeUpdate();
            //conn.commit();
            if (rowCount > 0) { }
            /*    System.out.println("Row updated " + rowCount + ", from: " +
                                   startIndex +
                                   " elapsed time: " +
                                   (System.currentTimeMillis() - elapsedTime));
            else
                System.out.println("No Row updated at: " + startIndex +
                                   " to: " + count +
                                   " elapsed time: " +
                                   (System.currentTimeMillis() - elapsedTime)); */
        } catch (SQLException s) {
            System.out.println("Exception when updating rows: " + startIndex +
                               " to: " + count + " - " +
                               s.getLocalizedMessage());
            //conn.rollback();
        }
        elapsedTime = System.currentTimeMillis() - elapsedTime;
        return (rowCount > 0) ? elapsedTime / rowCount : 0;
    }

    /**
     * find rows which F2 match again a text representation of n using scontains operator.
     * It only test for a result having 0 or more rows
     * @param row integer value converted to text to find
     * @throws SQLException If there are problems
     */
    public long findRows(int row, int batchSize) throws SQLException {
        ResultSet rs = null;
        long elapsedTime = 0;
        int n = row % 5373484 + 1; // range between 1 and Max Julian Date
        int startWindow = (n / batchSize) * batchSize;
        int endWindow = ((n / batchSize) + 1) * batchSize;
        try {
            findStatement.setInt(1, n);
            findStatement.setInt(2, startWindow);
            findStatement.setInt(3, endWindow);
            elapsedTime = System.currentTimeMillis();
            rs = findStatement.executeQuery();
            boolean found = rs.next();
            if (found) { }
            /*    System.out.println("Found rows with: " + n +
                                   " elapsed time: " +
                                   (System.currentTimeMillis() - elapsedTime) +
                                   " ms. window [" + startWindow + ".." +
                                   endWindow + "]");
            else
                System.out.println("Not Found rows with: " + n + " elapsed time: "+ (System.currentTimeMillis() - elapsedTime) + " ms."); */
        } catch (SQLException s) {
            System.err.println("Error during find Rows: " +
                               s.getLocalizedMessage());
            s.printStackTrace();
        } finally {
            if (rs != null)
                rs.close();
            rs = null;
        }
        elapsedTime = System.currentTimeMillis() - elapsedTime;
        return elapsedTime;
    }

    /**
     * count hits which F2 match again a text representation of n using scontains operator.
     * It only test for a result having 0 or more rows
     * @param n integer value converted to text to find
     * @throws SQLException If there are problems
     */
    public long countHits(int n) throws SQLException {
        CallableStatement myStatement = null;
        int hits = 0;
        long elapsedTime = 0;
        try {
            if (luceneMode) {
                myStatement =
                        conn.prepareCall("{call ? := SolrDomainIndex.countHits(?,to_char(to_date(?,'J'),'JSP'))}");
                myStatement.setString(2, LINDEX);
            } else {
                myStatement =
                        conn.prepareCall("{call ? := ctx_query.count_hits(?,to_char(to_date(?,'J'),'JSP'))}");
                myStatement.setString(2, OINDEX);
            }
            myStatement.registerOutParameter(1, Types.INTEGER);
            myStatement.setInt(3, n % 5373484 + 1);
            elapsedTime = System.currentTimeMillis();
            myStatement.execute();
            hits = myStatement.getInt(1);
            if (hits > 0) { }
            /*    System.out.println("Count hits with: " + (n % 5373484 + 1) +
                                   " elapsed time: " +
                                   (System.currentTimeMillis() - elapsedTime) +
                                   " ms. found: " + hits);
            else
                System.out.println("Not Found rows with: " + English.intToEnglish(n)+ " elapsed time: "+ stTime + " ms."); */
        } catch (SQLException s) {
            System.err.println("Error during find Rows: " +
                               s.getLocalizedMessage());
            s.printStackTrace();
        } finally {
            if (myStatement != null)
                myStatement.close();
            myStatement = null;
        }
        elapsedTime = System.currentTimeMillis() - elapsedTime;
        return elapsedTime;
    }

    /**
     * perform a sync operation at Lucene Domain Index applying pending changes
     * (inserts, updates). If there are errors, usually caused by another
     * transaction having an exclusive lock in a row being indexed,
     * it rollback the operation.
     * Next successful sync will apply pending changes of failed operations
     * @return a long value with the amount of milliseconds spent during sync
     * @throws SQLException If there are problems rollback the transaction
     */
    public long syncIndex() throws SQLException {
        CallableStatement myStatement = null;
        long elapsedTime = 0;
        try {
            if (luceneMode) {
                myStatement =
                        conn.prepareCall("{CALL SolrDomainIndex.sync(?)}");
                myStatement.setString(1, LINDEX);
            } else {
                // requires:
                // SQL> grant execute on ctx_ddl to scott;
                myStatement = conn.prepareCall("{CALL ctx_ddl.sync_index(?)}");
                myStatement.setString(1, OINDEX);
            }
            elapsedTime = System.currentTimeMillis();
            myStatement.execute();
            //conn.commit();
            /*if (luceneMode) {
                System.out.println("Index synced: " + LINDEX +
                                   " elapsed time: " + (System.currentTimeMillis() - elapsedTime) + " ms.");
            } else {
                System.out.println("Index synced: " + OINDEX +
                                   " elapsed time: " + (System.currentTimeMillis() - elapsedTime) + " ms.");
            }*/
        } catch (SQLException s) {
            System.out.println("Error at sync, ingnoring...., may be there are rows blocked by deleters " +
                               s.getLocalizedMessage());
            conn.rollback();
        } finally {
            if (myStatement != null)
                myStatement.close();
            myStatement = null;
        }
        elapsedTime = System.currentTimeMillis() - elapsedTime;
        return elapsedTime;
    }

    /**
     * perform an optimize operation at Lucene Domain Index merging segments in a new one.
     * If there are errors, usually caused by another transaction having an exclusive lock
     * on the index, it rollback the operation.
     * @return a long value with the amount of milliseconds spent during optimize
     * @throws SQLException If there are problems rollback the transaction
     */
    public long optimizeIndex() throws SQLException {
        CallableStatement myStatement = null;
        long stTime = 0;
        try {
            stTime = System.currentTimeMillis();
            if (luceneMode) {
              myStatement =
                      conn.prepareCall("{CALL SolrDomainIndex.optimize(?)}");
              myStatement.setString(1, LINDEX);
            } else {
              myStatement = conn.prepareCall("{CALL ctx_ddl.optimize_index(?,'FULL')}");
              myStatement.setString(1, OINDEX);
            }
            myStatement.execute();
            //conn.commit();
            if (luceneMode) {
                System.out.println("Index optimized: " + LINDEX +
                                   " elapsed time: " + (System.currentTimeMillis() - stTime) + " ms.");
            } else {
                System.out.println("Index optimized: " + OINDEX +
                                   " elapsed time: " + (System.currentTimeMillis() - stTime) + " ms.");
            }
        } catch (SQLException s) {
            System.out.println("Error at optimize, ingnoring...., may be there are rows blocked by deleters");
            conn.rollback();
        } finally {
            if (myStatement != null)
                myStatement.close();
            myStatement = null;
        }
        stTime = System.currentTimeMillis() - stTime;
        return stTime;
    }

}
