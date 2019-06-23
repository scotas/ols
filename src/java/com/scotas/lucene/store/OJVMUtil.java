package com.scotas.lucene.store;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.ODCI.AnyData;
import oracle.ODCI.ODCIEnv;
import oracle.ODCI.ODCIIndexInfo;
import oracle.ODCI.ODCIPartInfo;
import oracle.ODCI.ODCIPredInfo;
import oracle.ODCI.ODCIQueryInfo;


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
 * A database utility methods.
 *
 */
public class OJVMUtil {
    /** 
     * Constant used to get Logger name
     */
    static final String CLASS_NAME = OJVMUtil.class.getName();

    /**
     * Java Util Logging variables and default values 
     */
    private static Logger logger = Logger.getLogger(CLASS_NAME);

    static Connection dconn = null;

    static public void closeDbResources(PreparedStatement stmt, ResultSet rs) {
        try {
            if (rs != null)
                rs.close();
            if (stmt != null)
                stmt.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static public Connection getConnection() throws SQLException {
        if (dconn != null)
            return dconn;
        try {
            if(System.getProperty("java.vm.name").equals("JServer VM"))
              dconn = DriverManager.getConnection("jdbc:default:connection:");
            else { // only for testing purpose, it supose to be uses inside the OJVM
              DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
              dconn = DriverManager.getConnection("jdbc:oracle:thin:@"+
                                System.getProperty("db.str","localhost:1521:orcl"),
                                System.getProperty("db.usr","LUCENE"),
                                System.getProperty("db.pwd","LUCENE"));
            }
            dconn.setAutoCommit(false);
        } catch(SQLException s) {
          logger.throwing(CLASS_NAME, "getConnection", s);
          throw new InternalError(
             ".getConnection: Can't get defaultConnection "+s.getMessage());
        }
        return dconn;
    }

    public static void dumpIA(Logger log, ODCIIndexInfo ia) throws SQLException {
        Connection conn = null;
        CallableStatement cs = null;
        if (ia == null) // Sanity checks
          return;
        try {
            if (log.isLoggable(Level.FINER)) {
                conn = OJVMUtil.getConnection();
                cs = conn.prepareCall("begin sys.ODCIIndexInfoDump(?); end;");
                cs.setObject(1, ia);
                cs.execute();
            }
        } catch (SQLException s) {
            logger.throwing(CLASS_NAME, "dumpIA", s);
        } finally {
            OJVMUtil.closeDbResources(cs, null);
        }
    }

    public static void dumpQI(Logger log, ODCIQueryInfo qi) throws SQLException {
        Connection conn = null;
        CallableStatement cs = null;
        if (qi == null) // Sanity checks
          return;
        try {
            if (log.isLoggable(Level.FINER)) {
                conn = OJVMUtil.getConnection();
                cs = conn.prepareCall("begin sys.ODCIQueryInfoDump(?); end;");
                cs.setObject(1, qi);
                cs.execute();
            }
        } catch (SQLException s) {
            logger.throwing(CLASS_NAME, "dumpQI", s);
        } finally {
            OJVMUtil.closeDbResources(cs, null);
        }
    }

    public static void dumpOP(Logger log, ODCIPredInfo op) throws SQLException {
        Connection conn = null;
        CallableStatement cs = null;
        if (op == null) // Sanity checks
          return;
        try {
            if (log.isLoggable(Level.FINER)) {
                conn = OJVMUtil.getConnection();
                cs = conn.prepareCall("begin sys.ODCIPredInfoDump(?); end;");
                cs.setObject(1, op);
                cs.execute();
            }
        } catch (SQLException s) {
            logger.throwing(CLASS_NAME, "dumpOP", s);
        } finally {
            OJVMUtil.closeDbResources(cs, null);
        }
    }

    public static void dumpEnv(Logger log, ODCIEnv env) throws SQLException {
        Connection conn = null;
        CallableStatement cs = null;
        if (env == null) // Sanity checks
          return;
        try {
            if (log.isLoggable(Level.FINER)) {
                conn = OJVMUtil.getConnection();
                cs = conn.prepareCall("begin sys.ODCIEnvDump(?); end;");
                cs.setObject(1, env);
                cs.execute();
            }
        } catch (SQLException s) {
            logger.throwing(CLASS_NAME, "dumpEnv", s);
        } finally {
            OJVMUtil.closeDbResources(cs, null);
        }
    }

    public static void dumpPartInfo(Logger log, ODCIPartInfo pinfo) throws SQLException {
      Connection conn = null;
      CallableStatement cs = null;
      if (pinfo == null) // Sanity checks
        return;
      try {
        if (log.isLoggable(Level.FINER)) {
            conn = OJVMUtil.getConnection();
            cs = conn.prepareCall("begin sys.ODCIPartInfoDump(?); end;");
            cs.setObject(1, pinfo);
            cs.execute();
        }
      } catch (SQLException s) {
        logger.throwing(CLASS_NAME, "dumpPartInfo", s);
      } finally {
            OJVMUtil.closeDbResources(cs, null);
      }
    }

    public static String getAnyDataValue(AnyData a) throws SQLException {
        String typeName = a.gettypename();
        if ("SYS.NUMBER".equals(typeName))
            return a.AccessNumber().toString();
        else if ("SYS.BINARY_DOUBLE".equals(typeName))
            return a.AccessBDouble().toString();
        else if ("SYS.BINARY_FLOAT".equals(typeName))
            return a.AccessBFloat().toString();
        else if ("SYS.VARCHAR".equals(typeName))
            return a.AccessVarchar();
        else if ("SYS.VARCHAR2".equals(typeName))
            return a.AccessVarchar2();
        else if ("SYS.NVARCHAR2".equals(typeName))
            return a.AccessNVarchar2().toString();
        else if ("SYS.CHAR".equals(typeName))
            return a.AccessChar();
        else if ("SYS.NCHAR".equals(typeName))
            return a.AccessNchar().toString();
        else if ("SYS.DATE".equals(typeName))
            return a.AccessDate().toString();
        else if ("SYS.TIMESTAMP".equals(typeName))
            return a.AccessTimestamp().toString();
        else if ("SYS.TIMESTAMPLTZ".equals(typeName))
            return a.AccessTimestampLTZ().toString();
        else if ("SYS.TIMESTAMPTZ".equals(typeName))
            return a.AccessTimestampTZ().toString();
        else if ("SYS.INTERVALYM".equals(typeName))
            return a.AccessIntervalYM();
        else if ("SYS.INTERVALDS".equals(typeName))
            return a.AccessIntervalDS();
        else
            throw new SQLException("getAnyDataValue: NOT a Scalar Type in AnyData: " + typeName); 
    }
    
    public static void logSQLError(java.math.BigDecimal errCode, String errMsg) {
      logger.warning("SQLError code: " + errCode + " SQLError msg: " + errMsg);
    }
} 
