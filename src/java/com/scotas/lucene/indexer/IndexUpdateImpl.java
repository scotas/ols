package com.scotas.lucene.indexer;

import java.io.IOException;

import java.rmi.RemoteException;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import com.scotas.lucene.store.OJVMDirectory;
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

public class IndexUpdateImpl implements IndexUpdate {
    /**
     * Java Util Logging variables and default values
     */
    private Logger logger = null;

    private boolean closeWriters = true;

    private Hashtable openedWriters = new Hashtable();

    private Hashtable openedDirs = new Hashtable();

    private Hashtable counters = new Hashtable();

    /**
     * Constant used to get Logger name
     */
    static final String CLASS_NAME = IndexUpdateImpl.class.getName();

    /**
     * @param closeWriters
     */
    public IndexUpdateImpl(boolean closeWriters) {
        super();
        logger = Logger.getLogger(CLASS_NAME);
        logger.setLevel(Level.WARNING);
        this.closeWriters = closeWriters;
    }

    /**
     * @param prefix
     * @param deleted
     * @param inserted
     * @throws RemoteException
     */
    public void sync(String prefix, String[] deleted,
                     String[] inserted) throws RemoteException {
        logger.entering(CLASS_NAME, "sync",
                        new Object[] { prefix, deleted, inserted });
        IndexWriter writer = null;
        OJVMDirectory dir = null;
        try {
            long elapsedTime = System.currentTimeMillis();
            synchronized (OJVMDirectory.class) {
                Parameters par = null;
                if (closeWriters) {
                    dir = OJVMDirectory.getDirectory(prefix);
                    writer = LuceneDomainIndex.getIndexWriterForDir(dir, false);
                    par = dir.getParameters();
                } else {
                    synchronized (counters) {
                        if (counters.containsKey(prefix)) {
                            dir = (OJVMDirectory)openedDirs.get(prefix);
                            writer = (IndexWriter)openedWriters.get(prefix);
                        } else {
                            dir = OJVMDirectory.getDirectory(prefix);
                            writer = LuceneDomainIndex.getIndexWriterForDir(dir,
                                                                           false);
                            openedDirs.put(prefix, dir);
                            openedWriters.put(prefix, writer);
                        }
                        par = dir.getParameters();
                        Integer autoCommitMaxTime = new Integer(par.getParameter("AutoCommitMaxTime", "10"));
                        counters.put(prefix, autoCommitMaxTime);
                    }
                }
                String logLevel = par.getParameter("LogLevel", "WARNING");
                if (logger.isLoggable(Level.INFO)) {
                  logger.setLevel(Level.parse(logLevel)); // Restore log level
                  logger.info("doing write operations on connection: " +
                              dir.getConnection());
                }
                if (deleted.length > 0) {
                    logger.info(".sync - starting deletion on " + prefix +
                                " # deletions: " + deleted.length);
                    // Do not open an index reader if there is nothing to delete
                    for (int i = 0; i < deleted.length; i++)
                        writer.deleteDocuments(new Term("rowid", deleted[i]));
                }
                if (inserted.length > 0) {
                    String col = par.getParameter("ColName");
                    String tableSchema = par.getParameter("TableSchema");
                    String tableName = par.getParameter("TableName");
                    String partition = par.getParameter("Partition");
                    // Do not open an index writer if there is nothing to insert
                    logger.info(".sync - starting insertions on " + prefix +
                                " # insertions: " + inserted.length);
                    UserDataStore userDataStore = LuceneDomainIndex.getUserDataStore(par);
                    boolean includeMasterColumn =
                        "true".equalsIgnoreCase(par.getParameter("IncludeMasterColumn",
                                                                 "true"));
                    String extraCols = par.getParameter("ExtraCols");
                    String extraTabs = par.getParameter("ExtraTabs");
                    String whereCondition = par.getParameter("WhereCondition");
                    boolean lockMasterTable =
                        "true".equalsIgnoreCase(par.getParameter("LockMasterTable",
                                                                 "true"));
                    TableIndexer index =
                        new TableIndexer(OJVMUtil.getConnection(), tableSchema,
                                         tableName, partition);
                    index.setUserDataStore(userDataStore);
                    index.setExtraColsStr(extraCols);
                    index.setExtraTabsStr(extraTabs);
                    index.setExtraWhereStr(whereCondition);
                    index.setLockMasterTable(lockMasterTable);
                    index.index(logger, writer, col, inserted,
                                includeMasterColumn);
                }
                if (closeWriters) {
                    writer.commit();
                    writer.waitForMerges();
                }
            }
            elapsedTime = System.currentTimeMillis() - elapsedTime;
            logger.info(".sync - done " + dir + " elapsedTime: " +
                        elapsedTime + " ms.");
            // after sync purge deleted documents
            //entry.getDirectory().purge();
        } catch (SQLException e) {
            RemoteException t =
                new RemoteException("Error syncing the index: " + dir, e);
            logger.throwing(CLASS_NAME, "sync", t);
            throw t;
        } catch (IOException e) {
            RemoteException t =
                new RemoteException("Error syncing the index: " + dir, e);
            logger.throwing(CLASS_NAME, "sync", t);
            throw t;
        } finally {
            if (closeWriters) {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        RemoteException t =
                            new RemoteException("Error syncing the index: " +
                                                dir, e);
                        logger.throwing(CLASS_NAME, "sync", t);
                        throw t;
                    } finally {
                        writer = null;
                    }
                }
                if (dir != null)
                    try {
                        dir.close();
                    } catch (IOException e) {
                        RemoteException t =
                            new RemoteException("Error syncing the index: " +
                                                dir, e);
                        logger.throwing(CLASS_NAME, "sync", t);
                        throw t;
                    } finally {
                        dir = null;
                    }
            }
        }
        logger.exiting(CLASS_NAME, "sync", prefix);
    }

    /**
     * @param dir
     * @throws RemoteException
     */
    public void forceClose(String dir) throws RemoteException {
        Connection conn;
        try {
            if (!closeWriters) { // Sanity checks
                synchronized (OJVMDirectory.class) {
                    synchronized (counters) {
                        if (counters.containsKey(dir)) { // Sanity checks
                            IndexWriter writer =
                                (IndexWriter)openedWriters.get(dir);
                            logger.info("commit on: " + writer);
                            writer.commit();
                            writer.waitForMerges();
                            writer.close();
                            OJVMDirectory oDir =
                                (OJVMDirectory)openedDirs.get(dir);
                            logger.info("close: " + oDir);
                            conn = oDir.getConnection();
                            oDir.close();
                            logger.info("commit: " + conn);
                            conn.commit();
                            openedWriters.remove(dir);
                            openedDirs.remove(dir);
                            counters.remove(dir);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            RemoteException t =
                new RemoteException("Error in forceClose: " + dir, e);
            logger.throwing(CLASS_NAME, "forceClose", t);
            throw t;
        } catch (CorruptIndexException e) {
            RemoteException t =
                new RemoteException("Error in forceClose: " + dir, e);
            logger.throwing(CLASS_NAME, "forceClose", t);
            throw t;
        } catch (IOException e) {
            RemoteException t =
                new RemoteException("Error in forceClose: " + dir, e);
            logger.throwing(CLASS_NAME, "forceClose", t);
            throw t;
        }
    }

    /**
     * @return
     */
    public Hashtable getCounters() {
        return counters;
    }
}
