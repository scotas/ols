package com.scotas.solr.index;

import java.io.IOException;

import java.math.BigDecimal;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;


/**
 */
public class TestDBIndexSearchDoc extends DBTestCase {
    private static final String qry = "twenty OR one OR three OR hundred";

    public TestDBIndexSearchDoc() throws SQLException {
        super();
    }
                
    public void setUp() throws SQLException {
        createTable();
        createIndex();
        insertRows(1,200);
        syncIndex();
    }

    public void testCountHits() throws IOException, SQLException {
        CallableStatement myCallableStatement = null;
        Connection conn = null;
        BigDecimal result;
        System.out.println("testCountHits()");
        try {
            conn = ods.getConnection();
            startTime = System.currentTimeMillis();
            myCallableStatement = 
                    conn.prepareCall("{ ? = call LuceneDomainIndex.countHits(?,?) }");
            myCallableStatement.registerOutParameter(1, Types.BIGINT);
            myCallableStatement.setString(2, LINDEX);
            myCallableStatement.setString(3, qry);
            myCallableStatement.execute();
            result = myCallableStatement.getBigDecimal(1);
            startTime = System.currentTimeMillis()-startTime;
            System.out.println("Excecution time: " + startTime + " ms.");
            System.out.println("Hits: "+result);
        } catch (SQLException s) {
            s.printStackTrace();
        } finally {
            if (myCallableStatement != null)
                myCallableStatement.close();
            if (conn != null)
                conn.close();
        }
    }

    public void testPagination() throws SQLException {
        PreparedStatement psmt = null;
        ResultSet rs = null;
        Connection conn = null;
        System.out.println("testPagination()");
        try {
            conn = ods.getConnection();
            startTime = System.currentTimeMillis();
            psmt = conn.prepareStatement(
                        " select /*+ DOMAIN_INDEX_SORT */ f1,lscore(1) sc,f2\n" +
                        "             from "+ TABLE +" where lcontains(f2,?,1)>0 \n" +
                        "             order by lscore(1) ASC");
            psmt.setString(1,"rownum:[" + 1 + " TO " + 20 + "] AND " + qry);
            rs = psmt.executeQuery();
            startTime = System.currentTimeMillis()-startTime;
            System.out.println("Excecution time: " + startTime + " ms.");
            while (rs.next()) {
                int id = rs.getInt(1);
                float score = rs.getFloat(2);
                String str = rs.getString(3);
                System.out.println(id+" Score: " + score + " str: " + str);
            }
        } catch (SQLException s) {
            s.printStackTrace();
        } finally {
            if (rs != null)
                rs.close();
            rs = null;
            if (psmt != null)
                psmt.close();
            psmt = null;
            if (conn != null)
                conn.close();
            conn = null;
        }
    }

    public void testFilterByOrderBy() throws SQLException {
        PreparedStatement psmt = null;
        ResultSet rs = null;
        Connection conn = null;
        System.out.println("testFilterByOrderBy()");
        try {
            conn = ods.getConnection();
            startTime = System.currentTimeMillis();
            psmt = conn.prepareStatement(
                        " select /*+ DOMAIN_INDEX_SORT */ f1,lscore(1) sc,f2\n" +
                        "             from "+ TABLE +" where lcontains(f2,?,'F1:DESC',1)>0");
            psmt.setString(1,"(" + qry + ") AND F1:[0100 TO 0120]");
            rs = psmt.executeQuery();
            startTime = System.currentTimeMillis()-startTime;
            System.out.println("Excecution time: " + startTime + " ms.");
            while (rs.next()) {
                int id = rs.getInt(1);
                float score = rs.getFloat(2);
                String str = rs.getString(3);
                System.out.println(id+" Score: " + score + " str: " + str);
            }
        } catch (SQLException s) {
            s.printStackTrace();
        } finally {
            if (rs != null)
                rs.close();
            rs = null;
            if (psmt != null)
                psmt.close();
            psmt = null;
            if (conn != null)
                conn.close();
            conn = null;
        }
    }

    public void testFilterBy() throws SQLException {
        PreparedStatement psmt = null;
        ResultSet rs = null;
        Connection conn = null;
        System.out.println("testFilterBy()");
        try {
            conn = ods.getConnection();
            startTime = System.currentTimeMillis();
            psmt = conn.prepareStatement(
                        " select /*+ DOMAIN_INDEX_SORT */ f1,lscore(1) sc,f2\n" +
                        "             from "+ TABLE +" where lcontains(f2,?,1)>0");
            psmt.setString(1,"(" + qry + ") AND F1:[0100 TO 0120]");
            rs = psmt.executeQuery();
            startTime = System.currentTimeMillis()-startTime;
            System.out.println("Excecution time: " + startTime + " ms.");
            while (rs.next()) {
                int id = rs.getInt(1);
                float score = rs.getFloat(2);
                String str = rs.getString(3);
                System.out.println(id+" Score: " + score + " str: " + str);
            }
        } catch (SQLException s) {
            s.printStackTrace();
        } finally {
            if (rs != null)
                rs.close();
            rs = null;
            if (psmt != null)
                psmt.close();
            psmt = null;
            if (conn != null)
                conn.close();
            conn = null;
        }
    }

    public void testFilterAll() throws SQLException {
        PreparedStatement psmt = null;
        ResultSet rs = null;
        Connection conn = null;
        System.out.println("testFilterAll()");
        try {
            conn = ods.getConnection();
            startTime = System.currentTimeMillis();
            psmt = conn.prepareStatement(
                        " select /*+ DOMAIN_INDEX_SORT */ f1,lscore(1) sc,f2\n" +
                        "             from "+ TABLE +" where lcontains(f2,?,'F1:DESC',1)>0");
            psmt.setString(1,"rownum:[" + 1 + " TO " + 10 + "] AND (" + qry + ") AND F1:[0100 TO 0120]");
            rs = psmt.executeQuery();
            startTime = System.currentTimeMillis()-startTime;
            System.out.println("Excecution time: " + startTime + " ms.");
            while (rs.next()) {
                int id = rs.getInt(1);
                float score = rs.getFloat(2);
                String str = rs.getString(3);
                System.out.println(id+" Score: " + score + " str: " + str);
            }
        } catch (SQLException s) {
            s.printStackTrace();
        } finally {
            if (rs != null)
                rs.close();
            rs = null;
            if (psmt != null)
                psmt.close();
            psmt = null;
            if (conn != null)
                conn.close();
            conn = null;
        }
    }

    public void tearDown() throws SQLException {
        dropIndex();
        dropTable();
    }
}


