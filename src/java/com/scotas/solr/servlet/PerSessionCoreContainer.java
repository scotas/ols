package com.scotas.solr.servlet;


import com.scotas.lucene.indexer.Parameters;
import com.scotas.lucene.store.OJVMUtil;
import com.scotas.solr.core.OLSDirectoryFactory;
import com.scotas.solr.util.OLSClassLoader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrCore;
import org.apache.solr.common.SolrException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public final class PerSessionCoreContainer {
    protected static Logger log =
        LoggerFactory.getLogger(PerSessionCoreContainer.class);

    private static final boolean preLoadCores = false;

    private static final String DEFAULT_DEFAULT_CORE_NAME = "LUCENE.TEST_IDX";

    private static final String SELECT_ALL_INDEXES =
        "select OWNER,INDEX_NAME from all_indexes WHERE ITYP_OWNER='LUCENE' AND ITYP_NAME='SOLRINDEX'";

    static private final CoreContainer coreContainer = getCoreContainer();

    private static Connection conn = null;

    protected static Map<String, SolrCore> cores =
        new LinkedHashMap<String, SolrCore>();

    private static InputStream generateSolrConfig() {
        StringBuffer sb =
            new StringBuffer("<?xml version='1.0' encoding='UTF-8'?>\n");
        sb.append("<solr persistent='false'>\n");
        sb.append("  <cores hostPort='9099' adminPath='/admin/cores' defaultCoreName='").append(DEFAULT_DEFAULT_CORE_NAME).append("'>\n");
        PreparedStatement stmt = null;
        ResultSet rs = null;
        if (preLoadCores)
            try {
                if (conn == null)
                    conn = OJVMUtil.getConnection();
                stmt = conn.prepareStatement(SELECT_ALL_INDEXES);
                rs = stmt.executeQuery();
                while (rs.next()) {
                    String coreName = rs.getString(1) + "." + rs.getString(2);
                    sb.append("    <core name='").append(coreName).append("' instanceDir='").append(OLSClassLoader.baseResourcePkg).append(coreName.replace('.',
                                                                                                                                                            '/')).append("' />\n");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                OJVMUtil.closeDbResources(stmt, rs);
            }
        sb.append("  </cores>\n");
        sb.append("</solr>");
        return new ByteArrayInputStream(sb.toString().getBytes());
    }

    private static synchronized CoreContainer getCoreContainer() {
        CoreContainer cc;
        if (coreContainer != null)
            return coreContainer;
        try {
            OLSClassLoader ocl = new OLSClassLoader();
            Thread.currentThread().setContextClassLoader(ocl);
            cc = new CoreContainer("solr");
            cc.load("solr", new InputSource(generateSolrConfig()));
            log.info("CoreContainer loaded");
        } catch (MalformedURLException e) {
            log.error("Exception when creating a new CoreContainer", e);
            throw new RuntimeException("Error initializing coreContainer" +
                                       e.getMessage());
        } catch (IOException e) {
            log.error("Exception when creating a new CoreContainer", e);
            throw new RuntimeException("Error initializing coreContainer" +
                                       e.getMessage());
        } catch (ParserConfigurationException e) {
            log.error("Exception when creating a new CoreContainer", e);
            throw new RuntimeException("Error initializing coreContainer" +
                                       e.getMessage());
        } catch (SAXException e) {
            log.error("Exception when creating a new CoreContainer", e);
            throw new RuntimeException("Error initializing coreContainer" +
                                       e.getMessage());
        }
        return cc;
    }

    public static void reload(String coreName) throws ParserConfigurationException,
                                                      IOException,
                                                      SAXException {
        if (coreName == null)
            coreName = "";
        synchronized (coreContainer) {
            try {
                // TODO: impplemnt here coreContainer.reload functionality with bugs:
                // See: https://issues.apache.org/jira/browse/SOLR-4037
                //OLSClassLoader ocl = new OLSClassLoader();
                //Thread.currentThread().setContextClassLoader(ocl);
                //coreContainer.reload(coreName);
                remove(coreName);
                //log.info("reloaded core: " + coreName);
            } catch (SolrException s) {
                log.error("Exception when reloading core: " + coreName, s);
            }
        }
    }

    public static void remove(String coreName) {
        if (coreName == null)
            coreName = "";
        synchronized (coreContainer) {
            if (!cores.containsKey(coreName)) // Sanity checks
                return;
            SolrCore core = coreContainer.remove(coreName);
            conn = ((OLSDirectoryFactory)core.getDirectoryFactory()).getConnection();
            if (core != null) { // sanity checks
                log.info("closing core: " + core.getName() + "] open count= " +
                         core.getOpenCount());
                core.close();
            }
            try {
                conn.rollback();
                Parameters.refreshCache(coreName);
                log.info("removed core: " + coreName);
            } catch (SQLException s) {
                log.error("Error when removing core: " + coreName, s);
            }
            cores.remove(coreName);
        }
    }

    private static SolrCore register(String coreName) throws ParserConfigurationException,
                                                            IOException,
                                                            SAXException {
        if (coreName == null)
            coreName = "";
        SolrCore core = null;
        synchronized (coreContainer) {
            OLSClassLoader ocl = new OLSClassLoader();
            Thread.currentThread().setContextClassLoader(ocl);
            core = PerSessionCoreContainer.coreContainer.create(new CoreDescriptor(PerSessionCoreContainer.coreContainer,
                                                                coreName,
                                                                OLSClassLoader.baseResourcePkg +
                                                                coreName.replace('.',
                                                                                 '/')));
            coreContainer.register(coreName, core, false);
            cores.put(coreName, core);
            log.info("registered core: " + core.getName() + "] open count= " +
                     core.getOpenCount());
        }
        return core;
    }

    public static SolrCore getCore(String coreName) throws ParserConfigurationException,
                                                           IOException,
                                                           SAXException {
        SolrCore core = null;
        if (coreName == null)
            coreName = "";
        synchronized (coreContainer) {
            core = cores.get(coreName);
            if (core != null)
                return core;
            core = coreContainer.getCore(coreName);
            if (core == null)
                core = register(coreName);
            log.info("returning core[" + core.getName() + "] open count= " +
                     core.getOpenCount());
        }
        return core;
    }

    public static String instanceId() {
        return coreContainer.toString();
    }

    public PerSessionCoreContainer() {
        super();
    }
}
