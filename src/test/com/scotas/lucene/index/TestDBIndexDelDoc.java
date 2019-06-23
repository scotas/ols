package com.scotas.lucene.index;

import java.sql.SQLException;


/**
 */
public class TestDBIndexDelDoc extends DBTestCase {
    public TestDBIndexDelDoc() throws SQLException {
      super();    
    }
    
    public void setUp () throws SQLException {
       createTable();
       createIndex(false);
       insertRows(1,500);
       syncIndex(); // warnup
    }

    public void testDelDoc() throws SQLException {
        deleteRows(1,10);
        deleteRows(11,100);
        deleteRows(101,500);
    }

    public void tearDown() throws SQLException {
       dropIndex();
       dropTable();
    }
}









