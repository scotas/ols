package com.scotas.lucene.index;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite("org.apache.lucene.index.AllTests");
        suite.addTestSuite(TestDBIndex.class);
        suite.addTestSuite(TestDBIndexAddDoc.class);
        suite.addTestSuite(TestDBIndexDelDoc.class);
        suite.addTestSuite(TestDBIndexParallel.class);
        suite.addTestSuite(TestDBIndexSearchDoc.class);
        return suite;
    }
}
