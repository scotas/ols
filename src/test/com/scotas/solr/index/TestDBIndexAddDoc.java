package com.scotas.solr.index;

import java.sql.SQLException;


/**
 */
public class TestDBIndexAddDoc extends DBTestCase {

     public TestDBIndexAddDoc() throws SQLException {
       super();    
     }
     
    public void setUp () throws SQLException {
        createTable();
        createIndex();
        syncIndex(); // warnup
    }

    public void testAddDoc() throws SQLException {
        long syncTime = 0;
        insertRows(1,10);
        syncTime = syncIndex();
        System.out.println("Avg Sync time: "+syncTime/10);
        insertRows(11,100);
        syncTime = syncIndex();
        System.out.println("Avg Sync time: "+syncTime/90);
        insertRows(101,500);
        syncTime = syncIndex();
        System.out.println("Avg Sync time: "+syncTime/400);
        insertRows(501,1000);
        syncTime = syncIndex();
        System.out.println("Avg Sync time: "+syncTime/500);
        insertRows(1001,2000);
        syncTime = syncIndex();
        System.out.println("Avg Sync time: "+syncTime/1000);
        insertRows(2001,5000);
        syncTime = syncIndex();
        System.out.println("Avg Sync time: "+syncTime/3000);
    }

    public void tearDown() throws SQLException {
        dropIndex();
        dropTable();
    }
}









