package com.scotas.lucene.store;

import java.io.FileNotFoundException;
import java.io.IOException;

import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import oracle.sql.BLOB;

import com.scotas.lucene.util.StringUtils;


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
 * a pseudo file that represents a database entry.
 *  This class encapsulate the concept of File, but reading and storing the content
 *  in a BLOB column of the table lucene_files.
 *  To improve the direct access to the BLOB IO when this file is opened for writting
 *  creates a Temporary BLOB and insert it at closing time.
 *
 */
public class OJVMFile {

    private static final String INSERT_STATEMENT =
        "INSERT /*+ SYS_DL_CURSOR */ INTO %IDX%$T (NAME,LAST_MODIFIED,FILE_SIZE,DATA,DELETED) VALUES (?,sysdate,?,?,'N')";

    private static final String SELECT_STATEMENT =
        "SELECT LAST_MODIFIED,FILE_SIZE,DATA FROM %IDX%$T WHERE NAME=?";

    private Timestamp lastModified;

    private String name;

    private String prefix;

    private long size;

    private Connection conn;

    private Blob blob = null;

    private boolean opennedForWritting = false;

    /**
     * @param conn an opened database connection
     * @param prefix is the directory name where the inverted index files are stored
     * @param name of the file to be opened in read or write mode.
     * If you call to @see loadData() the file is openend in read only mode
     * otherwise you can write the data into the Vector() data.
     */
    public OJVMFile(Connection conn, String prefix, String name) {
        this.conn = conn;
        this.prefix = prefix;
        this.name = name;
        this.blob = null;
        this.opennedForWritting = false;
    }

    /**
     * openLocatorForReading() read the Blob locator
     * @see OJVMIndexInput
     */
    public void openLocatorForReading() throws IOException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(StringUtils.replace(SELECT_STATEMENT,"%IDX%",this.prefix));
            stmt.setString(1, this.name);
            rs = stmt.executeQuery();
            if (rs.next()) {
                this.setLastModified(rs.getTimestamp(1));
                this.setSize(rs.getLong(2));

                blob = rs.getBlob(3);
                if (blob == null)
                    throw new IOException("Failed to get lucene index blob='" +
                                               this.prefix + "' name='" +
                                               this.name + "'");
            } else
                throw new FileNotFoundException("Failed to open file for reading prefix='" +
                                           this.prefix + "' name='" +
                                           this.name + "'");
            this.opennedForWritting = false;
        } catch (SQLException e) {
            throw new IOException(e.getLocalizedMessage());
        } finally {
            OJVMUtil.closeDbResources(stmt, rs);
        }
    }


    /**
     * createLocatorForWritting()
     * create a Blob locator for writting
     * @see OJVMIndexOutput
     */
    public void openLocatorForWritting() {
        try {
            blob = BLOB.createTemporary(conn, true, BLOB.DURATION_SESSION);
            this.opennedForWritting = true;
            this.size = 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return Timestamp with the lastModified information about the file
     */
    public Timestamp getLastModified() {
        return lastModified;
    }

    /**
     * @return an String with the name of the file
     */
    public String getName() {
        return name;
    }

    /**
     * @return an String with the prefix of the file (directory)
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @return long value with the size of the file
     */
    public long getSize() {
        return size;
    }

    /**
     * Update last modification time of the file
     * @param lastModified
     */
    public void setLastModified(Timestamp lastModified) {
        this.lastModified = lastModified;
    }

    /** set file name
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /** set file prefix
     * @param prefix
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /** set file size
     * @param size
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     *  Files openned for writting use an Oracle temporary BLOB
     *  at closing time this BLOB is inserted into SCHEMA.IDXNAME$T table with his
     *  metadata information
     * @throws IOException
     */
    public void close() throws IOException {
        //System.out.println("OJVMFile.close "+this.prefix+"/"+this.name);
        if (this.opennedForWritting) {
            CallableStatement cs = null;
            try {
                cs = conn.prepareCall(StringUtils.replace(INSERT_STATEMENT,"%IDX%",this.getPrefix()));
                cs.setString(1, this.getName());
                cs.setLong(2, this.getSize());
                cs.setBlob(3, this.getBlob());
                int rowCount = cs.executeUpdate();
                if (rowCount != 1)
                    throw new RuntimeException("Failed to insert lucene index blob '" +
                                               this.prefix + "' name='" +
                                               this.name + "'");
            } catch (SQLException e) {
                throw new RuntimeException(e);
                  } finally {
                    OJVMUtil.closeDbResources(cs, null);
                  }
            try {
                ((BLOB)blob).freeTemporary();
            } catch (SQLException s) {
                throw new IOException(".close - failed to free temporary BLOB - " +
                                      s.getLocalizedMessage());
            } finally {
                this.blob = null;
            }
        } else {
            try {
                if (blob != null && ((BLOB)blob).isOpen()) {
                  ((BLOB)blob).close();
                  //System.out.println("this.blob.close() - " + this.blob);
                }
            } catch (SQLException e) {
                throw new IOException(".close - failed to close BLOB - " +
                                      e.getLocalizedMessage());
            } finally {
                this.blob = null;
            }
        }
    }

    /**
     * @return Blob locator for reading or writting
     * @see #openLocatorForReading
     * @see #openLocatorForWritting
     */
    public Blob getBlob() {
        return blob;
    }
}
