rem requires junit-3.8.2.jar loaded on sys's schema with -r -v -g public -s
rem or in the user's schema
rem as SYS grants these permitions to PUBLIC or to an specific DB user
rem dbms_java.grant_permission( 'LUCENE','SYS:java.io.FilePermission', '/junit.properties', 'read' );
rem dbms_java.grant_permission( 'LUCENE','SYS:java.lang.RuntimePermission', 'getClassLoader', '' );
rem dbms_java.grant_permission( 'LUCENE','SYS:java.lang.RuntimePermission', 'accessDeclaredMembers', '' );




create or replace java source named "junit.runner.OJVMTestSuiteLoader" as
package junit.runner;

import oracle.aurora.vm.OracleRuntime;

/**
 * The standard test suite loader. It can only load the same class once.
 */
public class OJVMTestSuiteLoader implements TestSuiteLoader {
    /**
     * Uses the system class loader to load the test class
     */
    public Class load(String suiteClassName) throws ClassNotFoundException {
            return Class.forName(suiteClassName, true, OracleRuntime.getCallerClass().getClassLoader());
    }
    /**
     * Uses the system class loader to load the test class
     */
    public Class reload(Class aClass) throws ClassNotFoundException {
            return aClass;
    }
}
/

create or replace java source named "junit.textui.OJVMRunner" as
package junit.textui;

import java.io.PrintWriter;
import java.io.StringWriter;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import junit.runner.BaseTestRunner;
import junit.runner.OJVMTestSuiteLoader;
import junit.runner.TestSuiteLoader;
import junit.runner.Version;


public class OJVMRunner extends BaseTestRunner {
    private XMLPrinter fPrinter;

    public static final int SUCCESS_EXIT = 0;

    public static final int FAILURE_EXIT = 1;

    public static final int EXCEPTION_EXIT = 2;

    /**
     * Constructs a OJVMRunner.
     */
    public OJVMRunner() {
        this(new PrintWriter(System.out));
    }

    /**
     * Constructs a OJVMRunner using the given stream for all the output
     */
    public OJVMRunner(PrintWriter writer) {
        this(new XMLPrinter(writer));
    }

    /**
     * Constructs a OJVMRunner using the given ResultPrinter all the output
     */
    public OJVMRunner(XMLPrinter printer) {
        fPrinter = printer;
    }

    /**
     * Runs a suite extracted from a TestCase subclass.
     */
    public static void run(Class testClass) {
        run(new TestSuite(testClass));
    }

    /**
     * Runs a single test and collects its results.
     * This method can be used to start a test run
     * from your program.
     * <pre>
     * public static void main (String[] args) {
     *     test.textui.OJVMRunner.run(suite());
     * }
     * </pre>
     */
    public static TestResult run(Test test) {
        OJVMRunner runner = new OJVMRunner();
        return runner.doRun(test);
    }

    /**
     * Always use the StandardTestSuiteLoader. Overridden from
     * BaseTestRunner.
     */
    public TestSuiteLoader getLoader() {
        return new OJVMTestSuiteLoader();
    }

    public void testFailed(int status, Test test, Throwable t) {
    }

    public void testStarted(String testName) {
    }

    public void testEnded(String testName) {
    }

    /**
     * Creates the TestResult to be used for the test run.
     */
    protected TestResult createTestResult() {
        return new TestResult();
    }

    public TestResult doRun(Test suite) {
        TestResult result = createTestResult();
        result.addListener(fPrinter);
        long startTime = System.currentTimeMillis();
        suite.run(result);
        long endTime = System.currentTimeMillis();
        long runTime = endTime - startTime;
        fPrinter.print(result, runTime);
        return result;
    }

    public static void main(String[] args) {
        System.out.println(getStringVal(args[0],args[1]));
    }

    public static String getStringVal(String classOrMethod, String clazz) {
        StringWriter sw = new StringWriter();
        OJVMRunner aTestRunner = new OJVMRunner(new PrintWriter(sw));
        try {
            String [] args = new String[2];
            args[0] = "-"+classOrMethod;
            args[1] = clazz;
            aTestRunner.start(args);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(EXCEPTION_EXIT);
        }
        return sw.getBuffer().toString();
    }

    /**
     * Starts a test run. Analyzes the command line arguments and runs the given
     * test suite.
     */
    public TestResult start(String[] args) throws Exception {
        String testCase = "";
        String method = "";

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-c"))
                testCase = extractClassName(args[++i]);
            else if (args[i].equals("-m")) {
                String arg = args[++i];
                int lastIndex = arg.lastIndexOf('.');
                testCase = arg.substring(0, lastIndex);
                method = arg.substring(lastIndex + 1);
            } else if (args[i].equals("-v"))
                System.err.println("JUnit " + Version.id() +
                                   " by Kent Beck and Erich Gamma");
            else
                testCase = args[i];
        }

        if (testCase.equals(""))
            throw new Exception("Usage: OJVMRunner [-wait] testCaseName, where name is the name of the TestCase class");

        try {
            if (!method.equals(""))
                return runSingleMethod(testCase, method);
            Test suite = getTest(testCase);
            return doRun(suite);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Could not create and run test suite: " + e);
        }
    }

    protected TestResult runSingleMethod(String testCase,
                                         String method) throws Exception {
        Class testClass = loadSuiteClass(testCase);
        Test test = TestSuite.createTest(testClass, method);
        return doRun(test);
    }

    protected void runFailed(String message) {
        System.err.println(message);
        System.exit(FAILURE_EXIT);
    }

    public void setPrinter(XMLPrinter printer) {
        fPrinter = printer;
    }


}
/

create or replace java source named "junit.textui.XMLPrinter" as
package junit.textui;

import java.io.PrintWriter;

import java.text.NumberFormat;

import java.util.Enumeration;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestFailure;
import junit.framework.TestListener;
import junit.framework.TestResult;

import junit.runner.BaseTestRunner;


public class XMLPrinter implements TestListener {
    PrintWriter fWriter;

    public XMLPrinter(PrintWriter writer) {
        fWriter = writer;
    }


    /* API for use by textui.TestRunner
     */

    synchronized void print(TestResult result, long runTime) {
        printHeader(runTime);
        printErrors(result);
        printFailures(result);
        printFooter(result);
    }

    void printWaitPrompt() {
        getWriter().println();
        getWriter().println("<RETURN> to continue");
    }

    /* Internal methods
     */

    protected void printHeader(long runTime) {
        getWriter().println("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>");
        getWriter().println("<junitreport>");
        getWriter()
            .println("  <time>" + elapsedTimeAsString(runTime) + "</time>");
    }

    protected void printErrors(TestResult result) {
        printDefects(result.errors(), result.errorCount(), "error");
    }

    protected void printFailures(TestResult result) {
        printDefects(result.failures(), result.failureCount(), "failure");
    }

    protected void printDefects(Enumeration booBoos, int count, String type) {
        if (count == 0)
            return;
        getWriter().println("  <" + type + "s total=\"" + count + "\">");
        for (int i = 1; booBoos.hasMoreElements(); i++) {
            printDefect((TestFailure)booBoos.nextElement(), i, type);
        }
        getWriter().println("  </" + type + "s>");
    }

    public void printDefect(TestFailure booBoo,
                            int count,
                            String type) { // only public for testing purposes
        printDefectHeader(booBoo, count, type);
        printDefectTrace(booBoo);
        printDefectFooter(count,type);
    }

    protected void printDefectHeader(TestFailure booBoo, int count, String type) {
        // I feel like making this a println, then adding a line giving the throwable a chance to print something
        // before we get to the stack trace.
        getWriter()
            .println("    <"+type+" count=\"" + count + "\" test=\"" + booBoo.failedTest() +
                     "\">");
    }

    protected void printDefectFooter(int count, String type) {
        // I feel like making this a println, then adding a line giving the throwable a chance to print something
        // before we get to the stack trace.
        getWriter().println("    </"+type+">");
    }

    protected void printDefectTrace(TestFailure booBoo) {
        getWriter()
            .println("      <![CDATA[\n        " + BaseTestRunner.getFilteredTrace(booBoo.trace()) +
                     "      ]]>");
    }

    protected void printFooter(TestResult result) {
        getWriter()
            .println("  <result status=\"" + ((result.wasSuccessful()) ? "OK" :
                                              "ERROR") + "\" run_count=\""+
                                              result.runCount()+ "\">");
        if (!result.wasSuccessful()) {
            getWriter()
                .println("    <failures " + 
                         "failure_count=\"" + result.failureCount() +
                         "\" error_count=\"" + result.errorCount() + "\"/>");
        }
        getWriter().println("  </result>");
        getWriter().println("</junitreport>");
    }


    /**
     * Returns the formatted string of the elapsed time.
     * Duplicated from BaseTestRunner. Fix it.
     */
    protected String elapsedTimeAsString(long runTime) {
        return NumberFormat.getInstance().format((double)runTime / 1000);
    }

    public PrintWriter getWriter() {
        return fWriter;
    }

    /**
     * @see junit.framework.TestListener#addError(Test, Throwable)
     */
    public void addError(Test test, Throwable t) {
        //getWriter().println("<error>"+test.toString()+"</error>");
    }

    /**
     * @see junit.framework.TestListener#addFailure(Test, AssertionFailedError)
     */
    public void addFailure(Test test, AssertionFailedError t) {
        //getWriter().println("<failure>"+test.toString()+"</failure>");
    }

    /**
     * @see junit.framework.TestListener#endTest(Test)
     */
    public void endTest(Test test) {
        //getWriter().println("</testing>");
    }

    /**
     * @see junit.framework.TestListener#startTest(Test)
     */
    public void startTest(Test test) {
        //getWriter().print("<testing>"+test.toString());
    }

}
/

CREATE OR REPLACE PROCEDURE OJVMRunner(s1 VARCHAR2, s2 VARCHAR2) AS LANGUAGE JAVA
NAME 'junit.textui.OJVMRunner.main(java.lang.String[])';
/
show errors
CREATE OR REPLACE FUNCTION OJVMRunnerAsString(s1 VARCHAR2, s2 VARCHAR2) return VARCHAR2
AS LANGUAGE JAVA
NAME 'junit.textui.OJVMRunner.getStringVal(java.lang.String, java.lang.String) return java.lang.String';
/
show errors

exit
