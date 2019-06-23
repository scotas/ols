package com.scotas.solr.core;


import com.scotas.lucene.indexer.Parameters;
import com.scotas.lucene.store.OJVMUtil;

import com.scotas.solr.odci.SolrDomainIndex;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.text.DateFormat;

import java.util.Date;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrEventListener;
import org.apache.solr.search.SolrIndexSearcher;


public class RowIDLoaderEventListener implements SolrEventListener {
    protected final SolrCore core;
    protected Connection conn;
    protected String action;
    protected boolean purgeDeletedFiles;
    protected Parameters par;

    public RowIDLoaderEventListener(SolrCore core) {
        this.core = core;
        try {
            this.conn = OJVMUtil.getConnection();
        } catch (SQLException e) {
            log.error("RowIDLoaderEventListener error getting current connection",
                      e);
        }
        log.debug("RowIDLoaderEventListener registered using connection: " +
                  this.conn);
    }

    public void postCommit() {
        try {
            if (log.isInfoEnabled())
                log.info("RowIDLoaderEventListener is commiting connection: " +
                         this.conn + " on core: " + this.core.getName() +
                         " timestamp: " +
                         DateFormat.getTimeInstance().format(new Date(System.currentTimeMillis())));
            if (purgeDeletedFiles) {
                if (log.isInfoEnabled())
                    log.info("RowIDLoaderEventListener is purging deleted files from: " +
                              this.core.getName());
                PreparedStatement stmt = null;
                try {
                    stmt = this.conn.prepareStatement("DELETE FROM " + this.core.getName() +
                           "$T WHERE DELETED = 'Y'");
                    stmt.execute();
                } catch (Exception e) {
                    log.error("RowIDLoaderEventListener error when purging deleted files",
                              e);
                } finally {
                    OJVMUtil.closeDbResources(stmt, null);
                }
            }
            this.conn.commit();
            SolrDomainIndex.notifySearchers(this.core.getName(), false);
        } catch (SQLException e) {
            log.error("RowIDLoaderEventListener error when commiting changes",
                      e);
        }
    }

    public void newSearcher(SolrIndexSearcher newSearcher,
                            SolrIndexSearcher currentSearcher) {
        //TODO: implements here notifySearchers functionality
        if (newSearcher != null)
            System.out.println("newSearcher != null");
        if (currentSearcher == null)
            System.out.println("currentSearcher == null");
    }

    public String toString() {
        return getClass().getName() + "[" + action + "] - purgeDeletedFiles[" +
            purgeDeletedFiles + "]";
    }

    public void init(NamedList args) {
        action = (String)args.get("action");
        purgeDeletedFiles =
                "true".equalsIgnoreCase((String)args.get("purgeDeletedFiles"));
        par = Parameters.getParameters(this.core.getName());
    }

    @Override
    public void postSoftCommit() {
        //TODO: SoftCommit requires notify??
    }
}
