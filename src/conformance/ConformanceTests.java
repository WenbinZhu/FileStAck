package conformance;

import test.*;

/** Runs all conformance tests on distributed filesystem components.

    <p>
    Tests performed are:
    <ul>
    <li>{@link conformance.common.PathTest}</li>
    <li>{@link conformance.rmi.SkeletonTest}</li>
    <li>{@link conformance.rmi.StubTest}</li>
    <li>{@link conformance.rmi.ConnectionTest}</li>
    <li>{@link conformance.rmi.ThreadTest}</li>
    <li>{@link conformance.storage.RegistrationTest}</li>
    <li>{@link conformance.storage.AccessTest}</li>
    <li>{@link conformance.storage.DirectoryTest}</li>
    <li>{@link conformance.naming.ContactTest}</li>
    <li>{@link conformance.naming.RegistrationTest}</li>
    <li>{@link conformance.naming.ListingTest}</li>
    <li>{@link conformance.naming.CreationTest}</li>
    <li>{@link conformance.naming.StubRetrievalTest}</li>
    </ul>
 */
public class ConformanceTests
{
    /** Runs the tests.

        @param arguments Ignored.
     */
    public static void main(String[] arguments)
    {
        // Create the test list, the series object, and run the test series.
        @SuppressWarnings("unchecked")
        Class<? extends Test>[]     tests =
            new Class[] {conformance.common.PathTest.class,
                         conformance.rmi.SkeletonTest.class,
                         conformance.rmi.StubTest.class,
                         conformance.rmi.ConnectionTest.class,
                         conformance.rmi.ThreadTest.class,
                         conformance.storage.RegistrationTest.class,
                         conformance.storage.AccessTest.class,
                         conformance.storage.DirectoryTest.class,
                         conformance.naming.ContactTest.class,
                         conformance.naming.RegistrationTest.class,
                         conformance.naming.ListingTest.class,
                         conformance.naming.CreationTest.class,
                         conformance.naming.StubRetrievalTest.class};
        Series                      series = new Series(tests);
        SeriesReport                report = series.run(3, System.out);

        // Print the report and exit with an appropriate exit status.
        report.print(System.out);
        System.exit(report.successful() ? 0 : 2);
    }
}
