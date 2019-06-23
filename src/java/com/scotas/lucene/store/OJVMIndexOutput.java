package com.scotas.lucene.store;

import java.io.IOException;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.lucene.store.BufferedIndexOutput;


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
 * A database implementation of IndexOuput using database storage OJVMFile
 *
 */
public class OJVMIndexOutput extends BufferedIndexOutput {

    private OJVMFile file;

    private long pos = 0;

    // remember if the file is open, so that we don't try to close it
    // more than once
    private volatile boolean isOpen;
    

    /**
     * Open a file for writting
     * to do this, creates a temporary BLOB and when its closed insert this BLOB
     * at SCHEMA.IDXNAME$T table
     * @param file
     */
    public OJVMIndexOutput(OJVMFile file) {
        this.file = file;
        this.pos = 0L;
        this.file.openLocatorForWritting();
        isOpen = true;
        //System.out.println(".OJVMIndexOutput - creating an empty file " +
        //                   this.file.getName() + " " + this.toString());
    }

    /**
     * Open a file for writing given an SQLConnection and his prefix and name
     * @param conn
     * @param prefix
     * @param name
     */
    public OJVMIndexOutput(Connection conn, String prefix, String name) {
        this(new OJVMFile(conn, prefix, name));
    }

    /** output methods: */
    public void flushBuffer(byte[] b, int offset, int size) throws IOException {
        try {
            //System.out.println(".flushBuffer - pos= " + (this.pos) + " offset= " + offset + " size= " + size + " " + this.file.getName());
            this.file.getBlob().setBytes(this.pos+1,b,offset,size);
            this.pos += (size-offset);
            if (this.file.getSize()<this.pos)
                this.file.setSize(this.pos);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void close() throws IOException {
      // only close the file if it has not been closed yet
      if (isOpen) {
        try {
          super.close();
        } finally {
          isOpen = false;
          //System.out.println(".close - " + this.file.getName() );
          this.file.close();
        }
      }
    }
    
    /** Random-access methods */
    public void seek(long pos) throws IOException {
        //if (this.pos > pos)
        //    System.out.println(".seek - new pos= " + (pos) + " old pos=" + this.pos + " "+ this.file.getName());
        super.seek(pos);
        this.pos = pos;
    }
    
    public long length() throws IOException {
      //System.out.println(".length - length= " + (file.getSize()) + " " + this.toString());
      return file.getSize();
    }
    
    public void setLength(long length) throws IOException {
      //System.out.println(".setLength - length= " + (length) + " " + this.toString());
      file.setSize(length);
    }
}
