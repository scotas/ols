package com.scotas.lucene.misc.ojvm;

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


import com.scotas.lucene.indexer.ContextManager;
import com.scotas.lucene.indexer.Entry;
import com.scotas.lucene.indexer.Parameters;
import com.scotas.lucene.store.OJVMDirectory;
import com.scotas.lucene.store.OJVMUtil;

import java.io.IOException;

import java.math.BigDecimal;

import java.sql.Connection;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import oracle.sql.STRUCT;
import oracle.sql.StructDescriptor;

import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRef;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.StrField;


/**
 * <code>IndexTerms</code> class extracts terms and their frequencies out
 * of an existing Lucene index.
 *
 */
public class IndexTerms implements SQLData {

    private BigDecimal key;

    /**
     * Java Util Logging variables and default values
     */
    private static Logger logger = null;

    /**
     * Constant used to get Logger name
     */
    static final String CLASS_NAME = IndexTerms.class.getName();

    static {
        logger = Logger.getLogger(CLASS_NAME);
        // default Log level, override it using LuceneDomainIndex.setLogLevel('level')
        logger.setLevel(Level.WARNING);
    }

    final static BigDecimal SUCCESS = new BigDecimal(0);
    final static BigDecimal ERROR = new BigDecimal(1);

    // Implement SQLData interface.
    String sql_type;

    public String getSQLTypeName() throws SQLException {
        return sql_type;
    }

    public void readSQL(SQLInput stream, String typeName) throws SQLException {
        sql_type = typeName;
        key = stream.readBigDecimal();
    }

    public void writeSQL(SQLOutput stream) throws SQLException {
        stream.writeBigDecimal(key);
    }

    static public BigDecimal ODCITableStart(STRUCT[] sctx, String indexName,
                                            String term) throws SQLException,
                                                                IOException {
        logger.entering(CLASS_NAME, "ODCITableStart",
                        new Object[] { sctx[0], indexName, term });
        int key;
        Connection conn = OJVMUtil.getConnection();
        Entry entry = OJVMDirectory.getCachedDirectory(indexName);
        Parameters pars = entry.getDirectory().getParameters();
        String logLevel = pars.getParameter("LogLevel", "WARNING");
        logger.setLevel(Level.parse(logLevel)); // Restore log level

        logger.info("Getting terms of field name: " + term + " using index: " +
                    indexName);
        Terms terms;
	if (term != null && term.length()>0)
        	terms = SlowCompositeReaderWrapper.wrap(entry.getReader()).terms(term);
	else
        	terms = SlowCompositeReaderWrapper.wrap(entry.getReader()).terms("rowid");
        TermsEnum termsEnum;
        termsEnum = terms.iterator(null);
        key = ContextManager.setContext(termsEnum);
        Object[] impAttr = new Object[1];
        impAttr[0] = new BigDecimal(key);
        StructDescriptor sd =
            new StructDescriptor("LUCENE.INDEX_TERM_OT", conn);
        sctx[0] = new STRUCT(sd, conn, impAttr);
        logger.info("key: " + key);
        logger.exiting(CLASS_NAME, "ODCITableStart", SUCCESS);
        return SUCCESS;
    }

    public BigDecimal ODCITableFetch(BigDecimal nrows,
                                     ARRAY[] outSet) throws SQLException,
                                                            InvalidTokenOffsetsException,
                                                            IOException {
        logger.entering(CLASS_NAME, "ODCITableFetch",
                        new Object[] { nrows, outSet });
        Connection conn = OJVMUtil.getConnection();
        // retrieve stored context using the key
        TermsEnum termsEnum =
            (TermsEnum)ContextManager.getContext(key.intValue());
        BytesRef term = null;
        term = termsEnum.next();
        CharsRef external = new CharsRef();
        FieldType ft = new StrField();
        // create a vector for the fetched rows
        int nterms = nrows.intValue();
        Vector v = new Vector(nterms);
        StructDescriptor outDesc =
            StructDescriptor.createDescriptor("LUCENE.TERM_INFO", conn);
        Object[] out_attr = new Object[3];
        do {
            if (term == null)
                break;
            int docFreq = termsEnum.docFreq();
            ft.indexedToReadable(term, external);
            out_attr[0] = external.toString();
            out_attr[1] = new BigDecimal(docFreq);
            v.add(new STRUCT(outDesc, conn, out_attr));
            term = termsEnum.next();
        } while (nterms-- > 0);
        // return if no terms found
        if (nterms == nrows.intValue())
            return SUCCESS;
        // create a HIGH_FREQ_TERM_OT instance and store the key in it
        Object out_arr[] = v.toArray();
        ArrayDescriptor ad = new ArrayDescriptor("LUCENE.TERM_INFO_SET", conn);
        outSet[0] = new ARRAY(ad, conn, out_arr);
        return SUCCESS;
    }

    public BigDecimal ODCITableClose() throws SQLException, IOException {

        // retrieve stored context using the key, and remove from ContextManager
        logger.entering(CLASS_NAME, "ODCITableClose", new Object[] { });
        TermsEnum termsEnum =
            (TermsEnum)ContextManager.clearContext(key.intValue());
        termsEnum = null;
        logger.exiting(CLASS_NAME, "ODCITableClose", SUCCESS);
        return SUCCESS;
    }
}


