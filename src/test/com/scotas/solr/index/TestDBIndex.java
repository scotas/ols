package com.scotas.solr.index;

import java.sql.SQLException;


/**
 */
public class TestDBIndex extends DBTestCase {

    public TestDBIndex() throws SQLException {
        super();
    }
                
    public void setUp() throws SQLException {
    }

    public void testDomainIndex() throws SQLException {
        createTable();
        createIndex();
        insertRows(10,49);
        long syncTime = syncIndex();
        System.out.println("Avg Sync time: "+syncTime/40);
        syncTime = optimizeIndex();
        System.out.println("Avg Optimize time: "+syncTime/40);
        deleteRows(10,49);
        dropIndex();
        dropTable();
    }

    public void tearDown() throws SQLException {
    }
}


