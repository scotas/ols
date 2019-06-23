package com.scotas.lucene.store;

import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

import java.util.Iterator;
import java.util.Map;

import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import com.scotas.lucene.indexer.Entry;
import com.scotas.lucene.indexer.LuceneDomainIndex;
import com.scotas.lucene.indexer.Parameters;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.SingleInstanceLockFactory;
import com.scotas.lucene.util.StringUtils;

import org.apache.lucene.index.DirectoryReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
 * Main entry point, extends the abstract&nbsp;class
 * @see Directory which encapsulates the access to the storage of the Lucene
 * inverted index.
 * The constructor gets an Oracle database connection with auto commit disabled,
 * then check for the existence of the tables  <strong>lucene_index</strong>
 * and. Also to reduce select operation on the lucene_index table creates
 * an Hashtable with the files stored under the prefix (directory) calling to the
 * method loadPreCachedFileNames().
 * When the user call the close() method the diretory object perform a database
 * commit only if its running outside the Oracle JVM.
 * The createOutput() method will create a new file for writing using
 * OJVMIndexOutput() stream, but the content of the new BLOB created will still
 * unsaved until the close() method is called into the OJVMFile, @see OJVMIndexOutput.
 * The openInput() method opens files for reading using OJVMIndexInput() stream
 * instance which automatically load the content of the BLOB and store it in a
 * Vector() instance to provide direct accces to the content.
 * deleteFile(), fileLength(), fileModified(), renameFile() and touchFile() performs
 * the respective operations on the files stored as rows into the lucene_files table.
 * list() method returns a array list representing all the files stored into
 * the current prefix (sub directory) represented by OJVMDirectory instance.
 * makeLock() method create a commit or write lock on this directory by returning
 * an instance of @see OJVMLock object.
 *
 */
public class OJVMDirectory extends Directory {
    private static final String purgeFilesStmt =
        "DELETE FROM %IDX%$T WHERE DELETED = 'Y'";

    private static final String getCachedSearcherStmt =
        "SELECT FILE_SIZE FROM %IDX%$T" +
                " WHERE NAME='updateCount'";

    private static final String updateCountStmt =
        "UPDATE %IDX%$T SET FILE_SIZE=FILE_SIZE+1 " +
        " WHERE NAME ='updateCount'";

    private static final String deleteFileStmt =
        "UPDATE %IDX%$T SET DELETED='Y' WHERE NAME=?";

    private static final String getFileLengthStmt =
        "SELECT FILE_SIZE FROM %IDX%$T " +
                "WHERE NAME=?";

    private static final String getFileModifiedStmt =
        "SELECT LAST_MODIFIED FROM %IDX%$T " +
                "WHERE NAME=?";

    private static final String loadPreCachedFileNamesStmt =
        "SELECT NAME FROM %IDX%$T WHERE DELETED='N'";

    private static final String touchFileStmt =
        "UPDATE %IDX%$T SET LAST_MODIFIED=sysdate" +
                " WHERE NAME=?";

    private static Logger log = LoggerFactory
        .getLogger(OJVMDirectory.class);
    
    private static Map cachedSearcher = null;

    private Connection conn = null;

    private Hashtable files = null;

    private String prefix = "_defaultPrefix";

    private boolean readOnly = true;

    /**
     * Creates a new <code>DBDirectory</code> instance using a given
     * @param prefix
     */
    public static OJVMDirectory getDirectory(String prefix) {
        return new OJVMDirectory(prefix);
    }

    public static void clearCachedSearcher() throws IOException {
        // clear out both the cached field, and the thunk so they don't
        // take up session space between calls
        if (cachedSearcher == null) // sanity checks
            return;
        Set openedSearcher = cachedSearcher.keySet();
        Iterator ite = openedSearcher.iterator();
        while (ite.hasNext()) {
            String searcherPrefix = (String)ite.next();
            Entry entry = (Entry)cachedSearcher.get(searcherPrefix);
            //System.out.println("Closing cached server '"+searcherPrefix+"'");
            entry.getDirectory().close();
        }
        cachedSearcher.clear();
        cachedSearcher = null;
    }

    public void removeCachedSearcher() throws IOException {
        if (cachedSearcher == null)
            return;
        Entry entry = (Entry)cachedSearcher.get(this.prefix);
        if (entry != null) {
            //System.out.println("removeCachedSearcher entry " + entry);
            cachedSearcher.remove(this.prefix);
            entry.close();
        }
    }

    public static void invalidateCachedEntry(String prefix) throws IOException {
        if (cachedSearcher != null)
            if (cachedSearcher.containsKey(prefix)) {
                Entry entry = (Entry)cachedSearcher.get(prefix);
                //System.out.println("invalidateCachedEntry entry " + entry);
                cachedSearcher.remove(prefix);
                entry.close();
            }
    }

    public static void invalidateCachedDirectory(Entry entry) throws IOException {
        //System.out.println("invalidateCachedDirectory entry " + entry);
        if (cachedSearcher != null)
            cachedSearcher.remove(entry.getDirectory().getPrefix());
        entry.close();
    }

    public static Entry getCachedDirectory(String prefix) throws IOException,
            SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = null;
        long updateCount = 0;
        try {
            conn = OJVMUtil.getConnection();
            stmt =
                conn.prepareStatement(StringUtils.replace(getCachedSearcherStmt,"%IDX%",prefix));
            rs = stmt.executeQuery();
            if (rs.next()) {
                updateCount = rs.getLong(1);
            } else
                throw new RuntimeException("getCachedSearcher can't find '" +
                                           prefix + "/updateCount' file");
        } catch (SQLException s) {
            throw s;
        } finally {
            OJVMUtil.closeDbResources(stmt, rs);
        }
        if (cachedSearcher == null)
            cachedSearcher = new HashMap();
        Entry entry = (Entry)cachedSearcher.get(prefix);
        if (entry == null ||
            updateCount != entry.getUpdateCount()) { // No cached entry, creates a new one
            if (entry != null && updateCount != entry.getUpdateCount())
                invalidateCachedDirectory(entry);
            OJVMDirectory dir = new OJVMDirectory(prefix);
            // Open an IndexReader read/write only used for deletions!!!
            IndexReader reader = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);
            Parameters parameters = Parameters.getParameters(prefix);
            Analyzer analyzer = LuceneDomainIndex.getAnalyzer(parameters);
            //TODO: check replacement latest Lucene impl. 
            /* String similarityMethod =
                parameters.getParameter("SimilarityMethod",
                                        "org.apache.lucene.search.DefaultSimilarity");
            Class clazz;
            try {
                clazz = similarityMethod.getClass().forName(similarityMethod);
                searcher.setSimilarity((Similarity)clazz.newInstance());
            } catch (ClassNotFoundException e) {
                System.err.println("Can not create an instance for SimilarityMethod: " +
                                   similarityMethod + " exception:\n" +
                        e.getMessage());
            } catch (InstantiationException e) {
                System.err.println("Can not create an instance for SimilarityMethod: " +
                                   similarityMethod + " exception:\n" +
                        e.getMessage());
            } catch (IllegalAccessException e) {
                System.err.println("Can not create an instance for SimilarityMethod: " +
                                   similarityMethod + " exception:\n" +
                        e.getMessage());
            }*/
            String columnName = parameters.getParameter("DefaultColumn");
            String dfltOp = parameters.getParameter("DefaultOperator", "OR");
            QueryParser parser =
                new QueryParser(LuceneDomainIndex.LUCENE_COMPAT_VERSION,
                                columnName, analyzer);
            parser.setDefaultOperator(("OR".equalsIgnoreCase(dfltOp)) ?
                                      QueryParser.OR_OPERATOR :
                                      QueryParser.AND_OPERATOR);
            boolean rewriteScore =
                "true".equalsIgnoreCase(parameters.getParameter("RewriteScore",
                                                                "false"));
            if (rewriteScore) {
                BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE); // to avoid
                // org.apache.lucene.search.BooleanQuery$TooManyClauses: maxClauseCount is set to 1024
                parser.setMultiTermRewriteMethod(MultiTermQuery.SCORING_BOOLEAN_QUERY_REWRITE);
            }
            entry = new Entry(dir, reader, searcher, analyzer, parser, updateCount);
            cachedSearcher.put(prefix, entry);
            //System.out.println("Creating getCachedDirectory entry "+entry);
        } // else
        //   System.out.println("Found a valid getCachedDirectory entry "+entry);
        return entry;
    }

    /**
     * default constructor to allow subclassing
     */
    protected OJVMDirectory() {
    }

    protected OJVMDirectory(String prefix) {
        //log.trace("OJVMDirectory(" + prefix + ")");
        this.prefix = prefix;
        this.files = new Hashtable();
        this.readOnly = true;
        try {
            this.setLockFactory(new SingleInstanceLockFactory());
            this.conn = OJVMUtil.getConnection();
        } catch (SQLException e) {
            throw new InstantiationError(e.getMessage());
        } catch (IOException e) {
          throw new InstantiationError(e.getMessage());
        }
        loadPreCachedFileNames();
        isOpen = true;
    }

    /**
     * After Directory creation, use this method to initialize
     * the parameters row with
     * @param param
     * and the updateCount row with 0
     */
    public void saveParameters(Parameters param) {
        param.save(this.prefix);
    }

    public Parameters getParameters() {
        return Parameters.getParameters(this.prefix);
    }

    /**
     * Closes the store.
     */
    public void close() throws IOException {
        //log.trace("close - " + this.prefix);
        isOpen = false;
        PreparedStatement stmt = null;
        try {
            if (!this.readOnly) {
                stmt = conn.prepareStatement(StringUtils.replace(updateCountStmt, "%IDX%",
                                          this.prefix));
                int update = stmt.executeUpdate();
                stmt.close();
                stmt = null;
                if (update == 0) {
                    log.warn("update == 0");
                    throw new IOException("close() failed to update " +
                                          this.prefix + "/updateCount row");
                }
                //System.out.println("Close update updateCount row");
            }
            if (!System.getProperty("java.vm.name").equals("JServer VM")) {
                //log.trace("commit() changes");
                conn.commit();
            }
        } catch (SQLException e) {
            log.warn("Exception on close()", e);
            throw new IOException(e.getMessage());
        } finally {
            //log.trace("set conn to null");
            conn = null;
        }
        //log.trace("set files to null");
        files.clear();
        files = null;
    }

    /**
     * Creates a new, empty pseudo file in the pseudo directory with the given name. Returns a
     * stream writing this file.
     */
    public IndexOutput createOutput(String name) throws IOException {
        if (fileExists(name))
            throw new IOException("Cannot overwrite: " + prefix + "/" + name);
        this.readOnly = false;
        OJVMFile file = new OJVMFile(this.conn, this.prefix, name);
        OJVMIndexOutput newFile = new OJVMIndexOutput(file);
        files.put(name, "");
        log.debug("createOutput: " + prefix + "/" + name);
        return newFile;
    }

    /**
     * Removes an existing file in the directory.
     */
    public void deleteFile(String name) throws IOException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(StringUtils.replace(deleteFileStmt, "%IDX%",
                                          this.prefix));
            stmt.setString(1, name);
            stmt.execute();
            files.remove(name);
            log.debug("deleteFile: " + prefix + "/" + name);
        } catch (Exception e) {
            log.warn("Exception on deleteFile", e);
            throw new IOException("cannot delete file " + name + ". Reason: " +
                                  e.getMessage());
        } finally {
            OJVMUtil.closeDbResources(stmt, null);
        }
        log.debug("deleteFile: " + prefix + "/" + name);
    }

    /**
     * Returns true iff a file with the given name exists.
     */
    public boolean fileExists(String name) throws IOException {
        return files.containsKey(name);
    }

    /**
     * Returns the length of a file in the directory.
     */
    public long fileLength(String name) throws IOException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(StringUtils.replace(getFileLengthStmt, "%IDX%",
                                          this.prefix));
            stmt.setString(1, name);
            rs = stmt.executeQuery();
            if (rs.next())
                return rs.getLong(1);
            else
                return 0;
        } catch (SQLException e) {
            throw new IOException("cannot verify file: " + name +
                                  ". Reason: " + e.getMessage());
        } finally {
            OJVMUtil.closeDbResources(stmt, rs);
        }
    }

    /**
     * Returns the time the named file was last modified.
     */
    public long fileModified(String name) throws IOException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(StringUtils.replace(getFileModifiedStmt, "%IDX%",
                                          this.prefix));
            stmt.setString(1, name);
            rs = stmt.executeQuery();
            rs.next();
            return rs.getTimestamp(1).getTime();
        } catch (Exception e) {
            throw new IOException("cannot read file modification timestamp for: " +
                                  name + ". Reason: " + e.getMessage());
        } finally {
            OJVMUtil.closeDbResources(stmt, rs);
        }
    }

    private void loadPreCachedFileNames() {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(StringUtils.replace(loadPreCachedFileNamesStmt, "%IDX%",
                                         this.prefix));
            rs = stmt.executeQuery();
            while (rs.next()) {
                String fileNameStr = rs.getString("NAME");
                if ("parameters".equals(fileNameStr) ||
                    "updateCount".equals(fileNameStr))
                    continue; // ignore internals files used by the OJVMDirectory
                files.put(fileNameStr, "");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            OJVMUtil.closeDbResources(stmt, rs);
        }
    }


    /** Returns an array of strings, one for each file in the
    *  directory.  This method does no
    *  filtering of the contents in a directory, and it will
    *  never return null (throws IOException instead).
    */
    public String[] listAll() throws IOException {
        Enumeration en = files.keys();
        String[] results = new String[files.size()];
        int i = 0;
        while (en.hasMoreElements())
            results[i++] = (String)en.nextElement();
        return results;
    }

    /**
     * Returns a stream reading an existing file.
     */
    public IndexInput openInput(String name) throws IOException {
        log.debug("openInput: " + prefix + "/" + name);
        return new OJVMIndexInput(new OJVMFile(this.conn, this.prefix, name));
    }

    /**
     * Set the modified time of an existing file to now.
     */
    public void touchFile(String name) throws IOException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(StringUtils.replace(touchFileStmt, "%IDX%",
                                          this.prefix));
            stmt.setString(1, name);
            stmt.execute();
        } catch (SQLException e) {
            throw new IOException("Unable to touch file " + name +
                                  ". Reason: " + e.getMessage());
        } finally {
            OJVMUtil.closeDbResources(stmt, null);
        }
    }

    /**
     * Purge deleted files on Lucene Domain Index table
     * @throws SQLException
     */
    public void purge() throws SQLException {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = OJVMUtil.getConnection();
            // Purge deleted documents.
            stmt = conn.prepareStatement(StringUtils.replace(purgeFilesStmt, "%IDX%",
                                          this.prefix));
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error in purge index: " + prefix +
                                       " - " + e.getMessage());
        } finally {
            if (stmt != null)
                stmt.close();
            stmt = null;
        }
    }

    public Connection getConnection() {
        return conn;
    }

    public String getPrefix() {
        return prefix;
    }

    /**
     * two directory are equals based only on his prefix name
     * @param obj
     * @return true or false if prefix are equals
     */
    public boolean equals(Object obj) {
        if (obj instanceof OJVMDirectory)
            return this.prefix == ((OJVMDirectory)obj).getPrefix();
        else
            return false;
    }

  /**
   * Ensure that any writes to these files are moved to
   * stable storage.  Lucene uses this to properly commit
   * changes to the index, to prevent a machine/OS crash
   * from corrupting the index.<br/>
   * <br/>
   * NOTE: Clients may call this method for same files over
   * and over again, so some impls might optimize for that.
   * For other impls the operation can be a noop, for various
   * reasons.
   */
    public void sync(Collection<String> names) throws IOException {
        //super.sync(names);
    }

    @Override
    public IndexOutput createOutput(String name, IOContext context) throws IOException {
        return this.createOutput(name);
    }

    @Override
    public IndexInput openInput(String name, IOContext context) throws IOException {
        return this.openInput(name);
    }

    public String toString() {
        return this.prefix;
    }
}
