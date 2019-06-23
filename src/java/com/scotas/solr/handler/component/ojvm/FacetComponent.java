package com.scotas.solr.handler.component.ojvm;


import com.scotas.lucene.indexer.ContextManager;
import com.scotas.lucene.indexer.Parameters;
import com.scotas.lucene.store.OJVMUtil;

import com.scotas.solr.odci.SolrDomainIndex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

import java.io.InputStreamReader;
import java.io.StringReader;

import java.math.BigDecimal;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import java.net.URLConnection;

import java.net.URLEncoder;

import java.sql.Connection;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

import java.util.ArrayList;
import java.util.Iterator;

import java.util.Map;

import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import oracle.sql.STRUCT;
import oracle.sql.StructDescriptor;

import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.solr.common.util.JavaBinCodec;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;

import org.slf4j.LoggerFactory;


public class FacetComponent implements SQLData {
    private BigDecimal key;

    /**
     * Constant used to get Logger name
     */
    static final String CLASS_NAME = FacetComponent.class.getName();

    /**
     * Java Util Logging variables and default values
     */
    private static org.slf4j.Logger logger =
        LoggerFactory.getLogger(CLASS_NAME);

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

    static public BigDecimal ODCITableStart(STRUCT[] sctx,
                                            String directoryPrefix,
                                            String facetQuery,
                                            String facetArgs) throws SQLException {
        logger.trace("ODCITableStart",
                     new Object[] { sctx[0], directoryPrefix, facetQuery,
                                    facetArgs });
        int key;
        FacetComponentContext ctx = new FacetComponentContext();
        Connection conn = OJVMUtil.getConnection();
        Parameters par = Parameters.getParameters(directoryPrefix);
        String queryString = facetQuery.trim();
        String searcherHost = null;
        if (queryString.startsWith("flashback:")) {
            int pos = queryString.indexOf(" AND ");
            if (pos < 0) {
                RuntimeException t =
                    new RuntimeException("Invalid Flashback syntax in sfacets() function, can not find AND conector syntax is 'flashback:nn AND restOfSolrQueryParserSyntax'");
                logger.error("ODCIStart", t);
                throw t;
            }
            String flashbackNumInfo = queryString.substring(10, pos).trim();
            queryString = queryString.substring(pos + 5);
            searcherHost = par.getFlashbackSearcherById(Integer.parseInt(flashbackNumInfo));
        } else
            searcherHost = par.getSolrRandomSearcher();
        URLConnection u = null;
        InputStream is = null;
        StringBuffer qryStr =
            new StringBuffer("http://" + searcherHost.replace('@', ':') +
                             "/select?wt=javabin&omitHeader=true&facet=true&rows=0");
        try {
            qryStr.append("&core=").append(URLEncoder.encode(directoryPrefix, "UTF-8"));
            qryStr.append("&q=").append(URLEncoder.encode(queryString, "UTF-8"));
            if (facetArgs != null && facetArgs.length() > 0) {
                qryStr.append("&").append(facetArgs);
            }
            //TODO: if we enable debugQuery there is more information to load
            //if (logger.isDebugEnabled())
            //    qryStr.append("&debugQuery=true");
            //System.out.println("URL to call: " + qryStr.toString());
            ctx.setStartTime(System.currentTimeMillis());
            u = new URL(qryStr.toString()).openConnection();
            is = u.getInputStream();
            SimpleOrderedMap m1 =
                (SimpleOrderedMap)new JavaBinCodec().unmarshal(is);
            SimpleOrderedMap facetCounts =
                (SimpleOrderedMap)m1.get("facet_counts");
            SimpleOrderedMap facetQueries =
                (SimpleOrderedMap)facetCounts.get("facet_queries");
            SimpleOrderedMap facetFields =
                (SimpleOrderedMap)facetCounts.get("facet_fields");
            SimpleOrderedMap facetDates =
                (SimpleOrderedMap)facetCounts.get("facet_dates");
            //Object o = facetCounts.get("facet_ranges");
            //System.out.println("facetRanges class: " + o.getClass() +
            //                   " value: " + o);
            SimpleOrderedMap facetRanges =
                (SimpleOrderedMap)facetCounts.get("facet_ranges");
            ctx.setFacetFieldIterator(facetFields.iterator());
            ctx.setFacetDateIterator(facetDates.iterator());
            ctx.setFacetQueryIterator(facetQueries.iterator());
            ctx.setFacetRangeIterator(facetRanges.iterator());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            SQLException sq =
                new SQLException("MalformedURLException doing http request: " +
                                 e.getLocalizedMessage());
            logger.error("ODCIStart", sq);
            throw sq;
        } catch (IOException e) {
            SQLException sq;
            if (u != null) {
                InputStream error = ((HttpURLConnection)u).getErrorStream();
                InputStreamReader ie = new InputStreamReader(error);
                BufferedReader br = new BufferedReader(ie);
                String read = null;
                StringBuffer sb = new StringBuffer();
                try {
                    while ((read = br.readLine()) != null) {
                        sb.append(read);
                    }
                    sq = new SQLException(sb.toString());
                } catch (IOException f) {
                    f.printStackTrace();
                    sq = new SQLException(e);
                }
            } else
                sq = new SQLException(e);
            logger.error("ODCIStart", sq);
            throw sq;
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException ioe) {
                // just going to ignore this one
                ioe.printStackTrace();
            }
        }
        key = ContextManager.setContext(ctx);
        Object[] impAttr = new Object[1];
        impAttr[0] = new BigDecimal(key);
        StructDescriptor sd =
            new StructDescriptor("LUCENE.FACET_TERM_OT", conn);
        sctx[0] = new STRUCT(sd, conn, impAttr);
        logger.trace("key",key);
        logger.trace("ODCITableStart", SUCCESS);
        return SUCCESS;
    }

    public BigDecimal ODCITableFetch(BigDecimal nrows,
                                     ARRAY[] outSet) throws SQLException,
                                                            InvalidTokenOffsetsException,
                                                            IOException {
        logger.trace("ODCITableFetch", new Object[] { nrows, outSet });
        FacetComponentContext ctx =
            (FacetComponentContext)ContextManager.getContext(key.intValue());
        Iterator<Map.Entry<String, Object>> itf = ctx.getFacetFieldIterator();
        Iterator<Map.Entry<String, Object>> itd = ctx.getFacetDateIterator();
        Iterator<Map.Entry<String, Object>> itr = ctx.getFacetRangeIterator();
        Iterator<Map.Entry<String, Object>> itq = ctx.getFacetQueryIterator();
        Connection conn = OJVMUtil.getConnection();
        int i = 0;
        //System.out.println("fetching: " + nrows);
        ArrayList resultArr = new ArrayList();
        while ((itf.hasNext() || itd.hasNext() || itr.hasNext() ||
                itq.hasNext()) && i < nrows.intValue()) {
            StructDescriptor outDesc =
                StructDescriptor.createDescriptor("LUCENE.FACET_INFO", conn);
            Object[] out_attr = new Object[5];
            ArrayDescriptor facetArrDesc =
                new ArrayDescriptor("LUCENE.NAME_VALUE_ARR", conn);
            ArrayList arrList = new ArrayList();
            StructDescriptor nameValueDesc =
                StructDescriptor.createDescriptor("LUCENE.NAME_VALUE", conn);
            Object[] nameValue = new Object[2];
            if (itf.hasNext()) {
                Map.Entry<String, Object> entry = itf.next();
                String name = entry.getKey();
                NamedList val = (NamedList)entry.getValue();
                out_attr[0] = name;
                Iterator<Map.Entry<String, Object>> j = val.iterator();
                while (j.hasNext()) {
                    Map.Entry<String, Object> facetEntry = j.next();
                    String facetName = facetEntry.getKey();
                    Integer facetValue = (Integer)facetEntry.getValue();
                    nameValue[0] = facetName;
                    nameValue[1] = facetValue;
                    arrList.add(new STRUCT(nameValueDesc, conn, nameValue));
                }
                // FACET_INFO.FACETS
                out_attr[1] = new ARRAY(facetArrDesc, conn, arrList.toArray());
            }
            arrList.clear();
            while (itq.hasNext()) {
                Map.Entry<String, Object> facetEntry = itq.next();
                String queryName = facetEntry.getKey();
                Integer queryValue = (Integer)facetEntry.getValue();
                nameValue[0] = queryName;
                nameValue[1] = queryValue;
                arrList.add(new STRUCT(nameValueDesc, conn, nameValue));
            }
            // FACET_INFO.QUERIES
            out_attr[2] = new ARRAY(facetArrDesc, conn, arrList.toArray());
            arrList.clear();
            if (itd.hasNext()) {
                Map.Entry<String, Object> entry = itd.next();
                String name = entry.getKey();
                NamedList val = (NamedList)entry.getValue();
                //System.out.println("val.class : " +
                //                   val.getClass() +
                //                   " val value: " + val);
                out_attr[0] =
                        (out_attr[0] == null) ? name : out_attr[0] + "," +
                        name;
                Iterator<Map.Entry<String, Object>> j = val.iterator();
                while (j.hasNext()) {
                    Map.Entry<String, Object> facetEntry = j.next();
                    String facetName = facetEntry.getKey();
                    Object facetValue = facetEntry.getValue();
                    //System.out.println("facetValue.class : " +
                    //                   facetValue.getClass() +
                    //                   " facetValue: " + facetValue);
                    if (facetValue instanceof Integer) {
                        nameValue[0] = facetName;
                        nameValue[1] = facetValue;
                    } else if (facetValue instanceof Float) {
                        nameValue[0] = facetName;
                        nameValue[1] = facetValue;
                    } else {
                        nameValue[0] = facetName + ":" + facetValue;
                        nameValue[1] = new Integer(0);
                    }
                    arrList.add(new STRUCT(nameValueDesc, conn, nameValue));
                }
                // FACET_INFO.DATES
                out_attr[3] = new ARRAY(facetArrDesc, conn, arrList.toArray());
            }
            arrList.clear();
            if (itr.hasNext()) {
                Map.Entry<String, Object> entry = itr.next();
                String name = entry.getKey();
                NamedList val = (NamedList)entry.getValue();
                //System.out.println("val.class : " +
                //                   val.getClass() +
                //                   " val value: " + val);
                out_attr[0] =
                        (out_attr[0] == null) ? name : out_attr[0] + "," +
                        name;
                Iterator<Map.Entry<String, Object>> j = val.iterator();
                while (j.hasNext()) {
                    Map.Entry<String, Object> facetEntry = j.next();
                    String facetName = facetEntry.getKey();
                    Object facetValue = facetEntry.getValue();
                    //System.out.println("facetValue.class : " +
                    //                   facetValue.getClass() +
                    //                   " facetValue: " + facetValue);
                    if (facetValue instanceof NamedList) {
                        Iterator<Map.Entry<String, Object>> k = ((NamedList)facetValue).iterator();
                        while (k.hasNext()) {
                            Map.Entry<String, Object> countEntry = k.next();
                            String countName = countEntry.getKey();
                            Object countValue = countEntry.getValue();
                            nameValue[0] = countName;
                            nameValue[1] = countValue;
                            arrList.add(new STRUCT(nameValueDesc, conn, nameValue));
                        }
                    } else if (facetValue instanceof Integer) {
                        nameValue[0] = facetName;
                        nameValue[1] = facetValue;
                        arrList.add(new STRUCT(nameValueDesc, conn, nameValue));
                    } else if (facetValue instanceof Float) {
                        nameValue[0] = facetName;
                        nameValue[1] = facetValue;
                        arrList.add(new STRUCT(nameValueDesc, conn, nameValue));
                    } else {
                        nameValue[0] = facetName + ":" + facetValue;
                        nameValue[1] = new Integer(0);
                        arrList.add(new STRUCT(nameValueDesc, conn, nameValue));
                    }
                }
                // FACET_INFO.RANGES
                out_attr[4] = new ARRAY(facetArrDesc, conn, arrList.toArray());
            }
            arrList.clear();
            resultArr.add(new STRUCT(outDesc, conn, out_attr));
            i++;
        }
        if (i > 0) { // if returned rows, return the array info set
            ArrayDescriptor ad =
                new ArrayDescriptor("LUCENE.FACET_INFO_SET", conn);
            outSet[0] = new ARRAY(ad, conn, resultArr.toArray());
            logger.info("returned rows: " + i);
        }
        return SUCCESS;
    }

    public BigDecimal ODCITableClose() throws SQLException, Throwable {

        // retrieve stored context using the key, and remove from ContextManager
        logger.trace("ODCITableClose", new Object[] { });
        FacetComponentContext ctx =
            (FacetComponentContext)ContextManager.clearContext(key.intValue());
        long elapsedTime = System.currentTimeMillis();
        logger.info("Elapsed time: " + (elapsedTime - ctx.getStartTime()));
        return SUCCESS;
    }
}
