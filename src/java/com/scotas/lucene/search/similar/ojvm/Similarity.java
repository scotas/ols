package com.scotas.lucene.search.similar.ojvm;

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

import java.io.BufferedReader;
import java.io.IOException;

import java.math.BigDecimal;

import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.ODCI.ODCIIndexCtx;

import oracle.sql.CLOB;

import oracle.xdb.XMLType;

import com.scotas.lucene.indexer.ContextManager;
import com.scotas.lucene.indexer.DefaultUserDataStore;
import com.scotas.lucene.indexer.Entry;
import com.scotas.lucene.indexer.LuceneDomainContext;
import com.scotas.lucene.indexer.LuceneDomainIndex;
import com.scotas.lucene.indexer.Parameters;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.spell.LevensteinDistance;


public class Similarity implements SQLData {
    private BigDecimal key;

    /**
     * Java Util Logging variables and default values
     */
    private static Logger logger = null;

    /**
     * Constant used to get Logger name
     */
    static final String CLASS_NAME = Similarity.class.getName();

    static {
        logger = Logger.getLogger(CLASS_NAME);
        // default Log level, override it using
        // LuceneDomainIndex.setLogLevel('level')
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

    /**
     * Return a value of the lsimilarity() for a particular XMLType
     * text.
     *
     * @param text
     * @param keyStr
     * @param ctx
     * @param sctx
     * @param scanflg
     * @return
     * @throws SQLException
     */
    public static java.math.BigDecimal TextSimilarity(XMLType text,
                                                      String keyStr,
                                                      String sortBy,
                                                      ODCIIndexCtx ctx,
                                                      LuceneDomainIndex[] sctx,
                                                      java.math.BigDecimal scanflg) throws SQLException,
                                                                                           IOException,
                                                                                           InvalidTokenOffsetsException,
                                                                                           ParseException {
        return TextSimilarity(text.getStringVal(), keyStr, sortBy, ctx, sctx,
                              scanflg);
    }

    /**
     * Return a value of the lsimilarity() for a particular CLOB
     * text.
     *
     * @param text
     * @param keyStr
     * @param ctx
     * @param sctx
     * @param scanflg
     * @return
     * @throws SQLException
     */
    public static java.math.BigDecimal TextSimilarity(CLOB text, String keyStr,
                                                      String sortBy,
                                                      ODCIIndexCtx ctx,
                                                      LuceneDomainIndex[] sctx,
                                                      java.math.BigDecimal scanflg) throws SQLException,
                                                                                           IOException,
                                                                                           InvalidTokenOffsetsException,
                                                                                           ParseException {
        String valueStr =
            (text != null) ? DefaultUserDataStore.readStream(new BufferedReader(text.characterStreamValue())) :
            "";
        return TextSimilarity(((valueStr != null) ? valueStr : ""), keyStr,
                              sortBy, ctx, sctx, scanflg);
    }

    /**
     * Return a value of the lsimilarity() for a particular String
     * text. We assume that OCIFetch function was called first. lsimilarity is
     * know as anciliary operator of lcontains, you can get the similarity with:
     * select lsimilarity(1) from tabname where lcontains(col,'text to find',1)>0
     * last number argument of lcontains match with the argument of lsimilarity.
     *
     * @param text column value of the row visited
     * @param keyStr query of lcontains
     * @param sortBy value of lcontains
     * @param ctx
     * @param sctx LuceneDomainIndexContext
     * @param scanflg
     * @return
     * @throws SQLException
     */
    public static java.math.BigDecimal TextSimilarity(String text,
                                                      String keyStr,
                                                      String sortBy,
                                                      ODCIIndexCtx ctx,
                                                      LuceneDomainIndex[] sctx,
                                                      java.math.BigDecimal scanflg) throws SQLException,
                                                                                           IOException,
                                                                                           ParseException {
        //logger.entering(CLASS_NAME, "TextSimilarity", new Object[] { text, keyStr, sortBy, ctx, sctx, scanflg });
        LuceneDomainContext sbtctx;
        int key;
        if (scanflg != null) { // Sanity checks
            SQLException t =
                new SQLException("Can not use lsimilarity() without lcontains() in a where side");
            logger.throwing(CLASS_NAME, "TextSimilarity", t);
            throw t;
        }
        if (sctx == null || sctx[0] == null) { // Sanity checks
            SQLException t =
                new SQLException("LuceneDomainIndex parameter is null. Are you using lcontains in a not index column?");
            logger.throwing(CLASS_NAME, "TextSimilarity", t);
            throw t;
        }
        key = sctx[0].getScanctx().intValue();
        // Get the resultSet back from the ContextManager using the key
        sbtctx = (LuceneDomainContext)ContextManager.getContext(key);

        Parameters pars = sbtctx.getEntry().getDirectory().getParameters();
        String logLevel = pars.getParameter("LogLevel", "WARNING");
        logger.setLevel(Level.parse(logLevel)); // Restore log level

        Entry entry = sbtctx.getEntry();

        String columnName = sbtctx.getColumn();

        // Only want the part of query that affects the main column
        QueryParser parser = entry.getParser();
        Query qry = parser.parse(keyStr);
        // Include the main column in que query
        String cleanKeyStr = qry.toString();

        String[] splitKeyStr = cleanKeyStr.split("\\s");
        StringBuffer inKeyStr = new StringBuffer();

        for (int i = 0; i < splitKeyStr.length; i++) {
            String k = splitKeyStr[i];
            logger.info("looking op in k: " + k);
            if (k.indexOf(columnName) >= 0) {
                int begin = k.indexOf(columnName) + columnName.length() + 1;
                int end;
                if (k.indexOf("~") > 0) {
                    end = k.indexOf("~");
                } else if (k.indexOf("*") == (k.length() - 1) ||
                           k.indexOf("?") == (k.length() - 1)) {
                    end = k.length() - 1;
                } else {
                    end = k.indexOf(")") > 0 ? k.length() - 1 : k.length();
                }
                k = k.substring(begin, end);
                inKeyStr.append(k).append(" ");
            }
        }

        String finalKeyStr = inKeyStr.toString().trim();


        float similarityValue = 0.0f;
        if (text != null && finalKeyStr != null) {

            LevensteinDistance distance = new LevensteinDistance();
            similarityValue =
                    distance.getDistance(text.toLowerCase(), finalKeyStr.toLowerCase());

            logger.info("Similarity between ROW: " + text.toLowerCase() +
                        " and TEXT: " + finalKeyStr.toLowerCase() + " is: " +
                        similarityValue);
        } else {
            logger.info("One of the given strings to calculate the distance is null");
        }

        BigDecimal retVal = new BigDecimal(similarityValue);
        logger.exiting(CLASS_NAME, "TextSimilarity", retVal);
        return retVal;
    }
}
