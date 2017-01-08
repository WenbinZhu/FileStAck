package test;

import java.io.*;
import java.util.*;

/** Test series report.

    <p>
    This class contains three lists: a list of tests that succeeded, a list of
    tests that were not run, and a list of reports from tests that failed. Tests
    not run are so because prerequisite tests did not complete successfully, or
    were not run at all.
 */
public class SeriesReport implements Serializable
{
    /** Successful tests. */
    private final List<Class<? extends Test>>   successful_tests;
    /** Tests that were not run due to missing prerequisites. */
    private final List<Class<? extends Test>>   abandoned_tests;
    /** Reports from tests that failed. */
    private final List<TestReport>              failure_reports;

    /** Creates a <code>SeriesReport</code>. */
    SeriesReport(List<Class<? extends Test>> successful_tests,
                 List<Class<? extends Test>> abandoned_tests,
                 List<TestReport> failure_reports)
    {
        this.successful_tests = successful_tests;
        this.abandoned_tests = abandoned_tests;
        this.failure_reports = failure_reports;
    }

    /** Returns <code>true</code> if and only if the test series completed
        successfully.

        <p>
        The test series is considered to have completed successfully if all
        tests have been run, and no tests failed.
     */
    public boolean successful()
    {
        return (abandoned_tests.size() == 0) && (failure_reports.size() == 0);
    }

    /** Prints a summary that includes the number of tests passed, failed, and
        not run.

        @param stream Stream to receive formatted output.
     */
    public void printSummary(PrintStream stream)
    {
        stream.println("passed: " + successful_tests.size() + "       " +
                       "failed: " + failure_reports.size() + "       " +
                       "not run: " + abandoned_tests.size());
    }

    /** Prints the test series report.

        <p>
        The report includes individual test reports for each failed test, a list
        of abandoned tests (if any), and a summary.

        @param stream Stream to receive formatted output.
     */
    public void print(PrintStream stream)
    {
        // Print the failed test reports.
        for(TestReport failed_test : failure_reports)
        {
            failed_test.print(stream);
            stream.println();
        }

        // If there are tests that were not run, list them.
        if(abandoned_tests.size() > 0)
        {
            stream.print("tests not run because of missing prerequisites: ");

            Iterator<Class<? extends Test>>     test_iterator =
                abandoned_tests.iterator();

            stream.print(test_iterator.next().getSimpleName());

            while(test_iterator.hasNext())
                stream.print(", " + test_iterator.next().getSimpleName());

            stream.println();
            stream.println();
        }

        // Print the test series summary.
        printSummary(stream);
    }
}
