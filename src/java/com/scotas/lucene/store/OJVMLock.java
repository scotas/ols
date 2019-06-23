package com.scotas.lucene.store;

import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockObtainFailedException;


/**
 * Copyright 2002-2006 The Apache Software Foundation
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
 * This implementation of {@link org.apache.lucene.store.Lock Lock} is
 * trivial as {@link OJVMDirectory} operations are managed by the Oracle
 * locking system.
 *
 */
public class

OJVMLock extends Lock {

    boolean isLocked = false;

    String name = null;
    String prefix = null;

    public OJVMLock(String prefix, String name) {
        if ("write.lock".equals(name)) {
            this.name = name;
            this.prefix = prefix;
        } else
            throw new RuntimeException("Only 'write.lock' is supported on :" +
                                       prefix);
    }

    public boolean obtain()  throws LockObtainFailedException, IOException {
        if (this.isLocked) // shortcut
            return true;
        ResultSet rs = null;
        PreparedStatement stmt = null;
        try {
            Connection conn = OJVMUtil.getConnection();
            stmt = conn.prepareStatement(
                       "select file_size from "+this.prefix+
                       "$T where name='updateCount' for update of file_size");
            stmt.execute();
            return this.isLocked = true;
        } catch (SQLException sqe) {
            //System.out.println(".obtain - sqe.getErrorCode(): "+sqe.getErrorCode());
            if (sqe.getErrorCode() == 30006)
                return this.isLocked = false;
            else
                throw new LockObtainFailedException(sqe.getLocalizedMessage());
        } finally {
            OJVMUtil.closeDbResources(stmt, rs);
        }
    }

    public void release() {
        //System.out.println(".release - " + this.name );
        isLocked = false;
    }

    public boolean isLocked() {
        return isLocked;
    }

    /**
     * @param lockWaitTimeout
     * @return
     * @throws LockObtainFailedException
     * @throws IOException
     */
    public boolean obtain(long lockWaitTimeout) throws LockObtainFailedException,
                                                       IOException {
        //System.out.println(".obtain - " + this.name + " lockWaitTimeout: " +
        //                   lockWaitTimeout);
        ResultSet rs = null;
        PreparedStatement stmt = null;
        try {
            Connection conn = OJVMUtil.getConnection();
            int waitSeconds = (int)(lockWaitTimeout / LOCK_POLL_INTERVAL);
            stmt = conn.prepareStatement(
                       "select file_size from "+this.prefix+
                       "$T where name='updateCount' for update of file_size  wait "+waitSeconds);
            stmt.execute();
            return this.isLocked = true;
        } catch (SQLException sqe) {
            //System.out.println(".obtain - sqe.getErrorCode(): "+sqe.getErrorCode());
            if (sqe.getErrorCode() == 30006)
                return this.isLocked = false;
            else
                throw new LockObtainFailedException(sqe.getLocalizedMessage());
        } finally {
            OJVMUtil.closeDbResources(stmt, rs);
        }
    }
}

