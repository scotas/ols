package com.scotas.solr.indexer;

import com.scotas.lucene.indexer.TestQueryHitsCtx;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite("com.scotas.solr.indexer.AllTests");
        suite.addTestSuite(TestQueryHits.class);
        suite.addTestSuite(TestQueryHitsCtx.class);
        return suite;
    }
}
