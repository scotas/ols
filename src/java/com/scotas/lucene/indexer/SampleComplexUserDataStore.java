package com.scotas.lucene.indexer;

import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.lucene.document.Document;
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


/**
 * This class is an example of how to implement an User Data Store function
 */
public class SampleComplexUserDataStore extends DefaultUserDataStore {
    private PreparedStatement st = null;
    
    public SampleComplexUserDataStore() throws SQLException {
        this.st = conn.prepareStatement("select f6,f7 from t2 where f5 = ?");
    }

    public Document getDocument(String rowid, 
                                           String col,
                                           Object value,
                                           String extraNames[], Object extraVals[])  throws SQLException, IOException {
        Document doc = super.getDocument(rowid,col,value,extraNames,extraVals);
        st.setString(1,(String)value);
        ResultSet rs = st.executeQuery();
        if (rs.next()) {
            doc.add(getField("t2.f6",rs.getObject(1),this.cachedFormaters));
            doc.add(getField("t2.f7",rs.getObject(1),this.cachedFormaters));
        }
        rs.close();
        //System.out.println(".getDocument - doc: "+doc);
        return doc;
    }

    protected void finalize() throws Throwable {
        this.st.close();
        super.finalize();
    }
}