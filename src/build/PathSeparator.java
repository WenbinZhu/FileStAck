package build;

/** Prints the classpath separator to standard output.

    <p>
    The separator between different directories in the classpath is
    <code>:</code> on non-Windows systems and <code>;</code> on Windows. The
    <code>java</code> command accepts only one of these separators on each
    platform. To allow the classpath to be set using the <code>-cp</code>
    option of the <code>java</code> command on both types of systems, the
    Makefile that is running the command retrieves the separator by running this
    build tool, and then uses it when issuing the command.
 */
public abstract class PathSeparator
{
    /** Program entry point.

        <p>
        This method ignores its arguments and prints the path separator.
     */
    public static void main(String[] arguments)
    {
        String  separator = System.getProperty("path.separator", ":");
        System.out.print(separator);
    }
}
