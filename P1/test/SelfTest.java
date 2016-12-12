package test;

/** Tests the testing library itself.

    <p>
    Tests performed are:
    <ul>
    <li>{@link test.InfiniteLoopTest}</li>
    <li>{@link test.TemporaryDirectoryTest}</li>
    </ul>
 */
public class SelfTest
{
    /** Runs the tests.

        @param arguments Ignored.
     */
    public static void main(String[] arguments)
    {
        // Create the test list, the series object, and run the test series.
        @SuppressWarnings("unchecked")
        Class<? extends Test>[]     tests =
            new Class[] {InfiniteLoopTest.class, TemporaryDirectoryTest.class};
        Series                      series = new Series(tests);
        SeriesReport                report = series.run(3, System.out);

        // Print the report and exit with an appropriate exit status.
        report.print(System.out);
        System.exit(report.successful() ? 0 : 2);
    }
}
