package com.scotas.lucene.indexer.remote;

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

import java.math.BigDecimal;

import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.scotas.lucene.indexer.IndexScan;
import com.scotas.lucene.indexer.IndexScanImpl;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import com.scotas.lucene.store.OJVMUtil;

import java.sql.SQLException;

/**
 *
 */
public class IndexScanServ extends UnicastRemoteObject implements IndexScan {

    /**
     *
     */
    private static IndexScanImpl localImpl = new IndexScanImpl();

    /**
     */
    private static boolean serverStarted = false;
    
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
    static final String CLASS_NAME = IndexScanServ.class.getName();

    static {
        logger = Logger.getLogger(CLASS_NAME);
        // default Log level, override it using
        // LuceneDomainIndex.setLogLevel('level')
        logger.setLevel(Level.WARNING);
    }

    /**
     * @throws RemoteException
     */
    public IndexScanServ() throws RemoteException {
        super();
    }

    /**
     * @param args
     * @throws RemoteException
     */
    public static void main(String[] args) throws RemoteException,
                                                  InterruptedException {
        logger.setLevel(Level.ALL);
        logger.entering(CLASS_NAME, "main", args);
        String srvPort        = (args.length > 0) ? args[0] : "1099";
        String logLevel       = (args.length > 1) ? args[1] : "WARNING";
        String jmxPort        = (args.length > 2) ? args[2] : "0";
        String sleepTimeout   = (args.length > 3) ? args[3] : "10000";
        jmxEnabled = ! "0".equalsIgnoreCase(jmxPort);
        // Create and install a security manager
        serverStarted = true;
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
            IndexScanServ obj = new IndexScanServ();
            LocateRegistry.createRegistry(Integer.parseInt(srvPort));

            logger.info("createRegistry listen on hostName: " + hostName + ":" + srvPort);

            Naming.rebind("//"+ hostName + ":" + srvPort + "/IndexScanServ", obj);

            logger.info("IndexScanImpl bound in registry name: " + hostName + ":" + srvPort);
        } catch (Exception e) {
            logger.throwing(CLASS_NAME, "IndexScanImpl error in Naming.rebind",
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
            if (jmxEnabled && dbVersion.indexOf("11g")>0) {
              // TODO: Customize this parameter
              logger.info("starting JMX aggent listening on port= " + jmxPort + " hostName: " + hostName);
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
              cs.setString(3, "IndexScanServ");
              cs.execute();
            } catch (SQLException s) {
              logger.warning("Background process already registered.");
            } finally {
              OJVMUtil.closeDbResources(cs, null);      
            }
            conn.commit();
            logger.info("IndexScanImpl sleepTimeout: " + sleepTimeout);
            logger.setLevel(Level.parse(logLevel));
            while(true) {
              //logger.info("waiting 1 second");
              // Do some clean-up with queries which took long time or was abondened by clients
              Thread.sleep(Integer.parseInt(sleepTimeout));
            }
        } catch (Exception e) {
            logger.throwing(CLASS_NAME, "IndexScanImpl error in Agent.startAgent()",
                            e);
        } finally {
            OJVMUtil.closeDbResources(cs, null);
            OJVMUtil.closeDbResources(stmt, rs); 
        }
        logger.exiting(CLASS_NAME, "main");
        logger.setLevel(Level.WARNING);
    }

    /**
     * @param dir
     * @param cmpval
     * @param sort
     * @param storeScore
     * @param firstRowHint
     * @return
     * @throws RemoteException
     */
    public int start(String dir, String cmpval, String sort,
                     boolean storeScore,
                     boolean firstRowHint) throws RemoteException {

        return localImpl.start(dir, cmpval, sort, storeScore, firstRowHint);
    }

    /**
     * @param ctx
     * @param nrows
     * @return
     * @throws RemoteException
     */
    public String[] fetch(int ctx, BigDecimal nrows) throws RemoteException {

        return localImpl.fetch(ctx, nrows);
    }

    /**
     * @param ctx
     * @throws RemoteException
     */
    public void close(int ctx) throws RemoteException {
        localImpl.close(ctx);
    }

    /**
     * @param ctx
     * @return
     * @throws RemoteException
     */
    public Query getQuery(int ctx) throws RemoteException {
        return localImpl.getQuery(ctx);
    }

    /**
     * @param ctx
     * @return
     * @throws RemoteException
     */
    public Hashtable getScoreList(int ctx) throws RemoteException {
        return localImpl.getScoreList(ctx);
    }

    /**
     * @param ctx
     * @return
     * @throws RemoteException
     */
    public TopDocs getHits(int ctx) throws RemoteException {
        return localImpl.getHits(ctx);
    }

   /**
     * @param ctx scan context
     * @param docId Lucene doc id
     * @return a BigDecimal value with score computed during start operation
     * @throws RemoteException
     */
    public BigDecimal getScoreValue(int ctx, int docId) throws RemoteException {
        return localImpl.getScoreValue(ctx,docId);
    }

    /**
     * @param dir
     * @throws RemoteException
     */
    public void refreshCache(String dir) throws RemoteException {
        localImpl.refreshCache(dir);
    }

    /**
     * @param dir
     * @param cmpval
     * @return
     * @throws RemoteException
     */
    public int getNumHits(String dir, String cmpval) throws RemoteException {
        return localImpl.getNumHits(dir, cmpval);
    }

    /**
     * @return true if this OJVM is working as remote server
     */
    public static boolean isServerStarted() {
        return serverStarted;
    }
}
