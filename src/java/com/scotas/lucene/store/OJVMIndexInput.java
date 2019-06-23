package com.scotas.lucene.store;

import java.io.IOException;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.lucene.store.BufferedIndexInput;


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
 * A database BufferedIndexInput implementation.
 * using OJVMFile storage
 *
 * @see OJVMFile
 */
public class OJVMIndexInput extends BufferedIndexInput {

    /**
     * The OJVMFile opened for reading
     */
    private OJVMFile file;

    /**
     * Current position of the input
     */
    private long position;
    boolean isClone;


    /**
     * Open an OJVMFile for reading
     * using the input paramters for creating a new instance
     * @param conn database connection
     * @param prefix and
     * @param name values
     * @see com.scotas.lucene.store.OJVMFile instance
     */
    public OJVMIndexInput(Connection conn, String prefix, String name) throws IOException {
        this(new OJVMFile(conn, prefix, name));
    }

    /**
     * Open an OJVMFile passed as parameter for reading
     * @param file
     * @see OJVMFile
     */
    public OJVMIndexInput(OJVMFile file) throws IOException {
        //System.out.println(".open: " + file.getName() + " - " + this.toString());
        //TODO: replace 8192 with the blocksize of the DB
        super("OJVMIndexInput(" + file.getName() + ")",8192);
        this.file = file;
        this.position = 0L;
        this.isClone = false;
        // populate BLOB locator from the database
        this.file.openLocatorForReading();
    }

    /**
     * Close this file opened for reading
     * @throws IOException if there where problem closing the
     * @see OJVMFile
     */
    public void close() throws IOException {
        // nothing to do
        if (!this.isClone) { 
            //System.out.println(".close: " + this.file.getName() + " - " + this.toString());
            this.file.close();
        }
    }

    public BufferedIndexInput clone() {
      //System.out.println(".clone: " + file.getName() + " - " + this.toString());
      OJVMIndexInput clone = (OJVMIndexInput)super.clone();
      clone.isClone = true;
      clone.position = 0L;
      //System.out.println(".clone: new clone " + clone.toString());
      return clone;
    }
    
    /**
     * @return a length() information of the Lucene file
     * @see OJVMFile
     */
    public long length() {
        //System.out.println(".length length= "+this.file.getSize()+" "+this.toString());
        return this.file.getSize();
    }

    /**
     * Read a portion of the Lucene file at the current position (pointer)
     * this method will call to Blob.getBytes(pointer + 1, length) method
     * to get direct access to a portion of the BLOB
     * This strategy seem to be faster than using a sequencial InputStream for
     * reading Blob and closing/opening it when seekInternal rewind the stream
     * @param dest the array to read bytes into
     * @param offset the offset in the array to start storing bytes
     * @param length the number of bytes to read
     * @throws IOException if there where errors during Blob IO operations
     * @see #seekInternal position
     */
     /** IndexInput methods */
    protected void readInternal(byte[] b, int offset,
                                int len) throws IOException {
        //System.out.println(".readInternal offset= "+offset+" len= "+len+" position= "+position+" "+this.toString());
        synchronized (this.file) {
            long position = getFilePointer();
            if (position != this.position) {
                this.position = position;
            }
            byte []c;
            try {
                c = this.file.getBlob().getBytes(position+1,len);
                if (c == null)
                    throw new IOException("read past EOF");
                //System.out.println(".readInternal - b.length= "+b.length+" c.length= "+c.length);
                System.arraycopy(c,0,b,offset,len);
                this.position += len;
            } catch (SQLException e) {
                if (e.getErrorCode() == 1555) {
                    this.file.close();
                    this.file.openLocatorForReading();
                    try {
                        c = this.file.getBlob().getBytes(position+1,len);
                        if (c == null)
                            throw new IOException("read past EOF");
                        //System.out.println(".readInternal - reload BLOB - b.length= "+b.length+" c.length= "+c.length);
                        System.arraycopy(c,0,b,offset,len);
                        this.position += len;
                    } catch(SQLException sqe) {
                        throw new IOException(sqe.getLocalizedMessage());
                    }
                } else
                    throw new IOException(e.getLocalizedMessage());
            }
        }
    }


    /**
     * Move current buffer position to a new one
     * @param position
     * @throws IOException
     */
     protected void seekInternal(long position) {
         //System.out.println(".seekInternal position= "+position+" "+this.toString());
     }
}
