package com.scotas.lucene.indexer;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite("org.apache.lucene.indexer.AllTests");
        suite.addTestSuite(TestQueryHits.class);
        suite.addTestSuite(TestQueryHitsCtx.class);
        return suite;
    }
}
