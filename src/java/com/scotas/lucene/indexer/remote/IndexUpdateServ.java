package com.scotas.lucene.indexer.remote;

import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.scotas.lucene.indexer.IndexUpdate;
import com.scotas.lucene.indexer.IndexUpdateImpl;
import com.scotas.lucene.store.OJVMUtil;

import java.sql.SQLException;


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

public class IndexUpdateServ extends UnicastRemoteObject implements IndexUpdate {
    public IndexUpdateServ() throws RemoteException {
        super();
    }

    /**
     *
     */
    private static IndexUpdateImpl localImpl = new IndexUpdateImpl(false);

    /**
     */
    private static boolean jmxEnabled = false;

    /**
     * Java Util Logging variables and default values
     */
    private static Logger logger = null;

    private static final String SELECT_HOSTNAME_STATEMENT =
        "SELECT SYS_CONTEXT('USERENV','SERVER_HOST') FROM dual";

    private static final String SELECT_VERSION_STATEMENT =
        "SELECT banner FROM v$version WHERE rownum=1";

    private static final String REG_BG_PROCESS_STATEMENT =
      "insert into LUCENE.BG_PROCESS values (?,?,?,SYS_CONTEXT('USERENV','ACTION'))";

    private static final String START_JMX_AGENT =
        "call dbms_java.start_jmx_agent(?, ?)";

    /**
     * Constant used to get Logger name
     */
    static final String CLASS_NAME = IndexUpdateServ.class.getName();

    static {
        logger = Logger.getLogger(CLASS_NAME);
        // default Log level, override it using
        // LuceneDomainIndex.setLogLevel('level')
        logger.setLevel(Level.WARNING);
    }

    /**
     * @param args
     * @throws RemoteException
     */
    public static void main(String[] args) throws RemoteException,
                                                  InterruptedException {
        logger.setLevel(Level.ALL);
        logger.entering(CLASS_NAME, "main", args);
        String srvPort        = (args.length > 0) ? args[0] : "1098";
        String logLevel       = (args.length > 1) ? args[1] : "WARNING";
        String jmxPort        = (args.length > 2) ? args[2] : "0";
        String commitTimeout  = (args.length > 3) ? args[3] : "100";
        jmxEnabled = ! "0".equalsIgnoreCase(jmxPort);
        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
        logger.info("SecurityManager= " + System.getSecurityManager());
        Connection conn;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String hostName = "localhost";
        try {
            conn = OJVMUtil.getConnection();
            stmt = conn.prepareStatement(SELECT_HOSTNAME_STATEMENT);
            rs = stmt.executeQuery();
            if (rs.next())
                hostName = rs.getString(1);
            IndexUpdateServ obj = new IndexUpdateServ();
            LocateRegistry.createRegistry(Integer.parseInt(srvPort));

            logger.info("createRegistry listen on hostName: " + hostName + ":" + srvPort);

            Naming.rebind("//" + hostName + ":" + srvPort + "/IndexUpdateServ", obj);

            logger.info("IndexUpdateImpl bound in registry name: " + hostName + ":" + srvPort);
        } catch (Exception e) {
            logger.throwing(CLASS_NAME, "IndexUpdateImpl error in Naming.rebind",
                            e);
        } finally {
            OJVMUtil.closeDbResources(stmt, rs);
        }
        String dbVersion = "10g";
        CallableStatement cs = null;
        try {
            conn = OJVMUtil.getConnection();
            stmt = conn.prepareStatement(SELECT_VERSION_STATEMENT);
            rs = stmt.executeQuery();
            if (rs.next())
                dbVersion = rs.getString(1);
            if (jmxEnabled && dbVersion.indexOf("11g") > 0) {
                // TODO: Customize this parameter
                logger.info("starting JMX aggent listening on port= " + jmxPort + " hostName: " +
                            hostName);
                cs = conn.prepareCall(START_JMX_AGENT);
                cs.setString(1, jmxPort);
                cs.setString(2, "false");
                cs.execute();
                cs.close();
                cs = null;
            }
            rs.close();
            rs = null;
            stmt.close();
            stmt = null;
            try {
              cs = conn.prepareCall(REG_BG_PROCESS_STATEMENT);
              cs.setString(1, hostName);
              cs.setInt(2, Integer.parseInt(srvPort));
              cs.setString(3, "IndexUpdateServ");
              cs.execute();
            } catch (SQLException s) {
              logger.warning("Background process already registered.");              
            } finally {
              OJVMUtil.closeDbResources(cs, null);      
            }
            conn.commit();
            logger.info("IndexUpdateImpl commitTimeout: " + commitTimeout);
            logger.setLevel(Level.parse(logLevel));
            while (true) {
                //logger.info("waiting 0.1 second");
                // Close writers which where unused for 10 seconds
                Thread.sleep(Integer.parseInt(commitTimeout));
                Hashtable counters = localImpl.getCounters();
                synchronized (counters) {
                    Enumeration ite = counters.keys();
                    while (ite.hasMoreElements()) {
                        String prefix = (String)ite.nextElement();
                        int cntInt =
                            ((Integer)counters.get(prefix)).intValue();
                        //logger.info("Directory: " + prefix + " cnt: " +
                        //            cntInt);
                        cntInt--;
                        if (cntInt == 0) {
                            localImpl.forceClose(prefix);
                        } else
                            localImpl.getCounters().put(prefix,
                                                        new Integer(cntInt));
                    }
                }
            }
        } catch (Exception e) {
            logger.throwing(CLASS_NAME,
                            "IndexUpdateImpl error in Agent.startAgent()", e);
        } finally {
            OJVMUtil.closeDbResources(cs, null);
            OJVMUtil.closeDbResources(stmt, rs);
        }
        logger.exiting(CLASS_NAME, "main");
        logger.setLevel(Level.WARNING);
    }

    public void sync(String dir, String[] deleted,
                     String[] inserted) throws RemoteException {
        localImpl.sync(dir, deleted, inserted);
    }

    public void forceClose(String dir) throws RemoteException {
        localImpl.forceClose(dir);
    }
}
