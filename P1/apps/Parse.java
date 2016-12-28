package apps;

/** Extracts the hostname or path portion of a remote path, and prints the
    result on standard output.

    <p>
    This application is primarily meant to be used by a script which wishes to
    set the <code>DFSHOST</code> and <code>DFSCWD</code> environment variables.
    The application runs the path parser on its argument, and prints either the
    hostname or the path portion of the result.

    <p>
    Thus, if the given path specifies a hostname, that hostname will be printed.
    But, if a hostname is not specified, the current value of
    <code>DFSHOST</code> is printed. Similarly, if the given path is absolute,
    the parsed absolute path is printed. But, if it is relative, the path is
    parsed relative to the current value of <code>DFSCWD</code>, and the result
    is printed.

    <p>
    These outputs are meant for direct use in a <code>cd</code>-like command.
    The application also takes care to return proper exit codes. If the result
    printed is valid, the exit code is zero. If there was a problem parsing the
    given path, the exit code is non-zero.

    <p>
    The application takes two arguments. The first is the remote path to be
    parsed, and the second is either <code>hostname</code> or <code>path</code>,
    indicating which portion of the result is to be printed.
 */
public class Parse extends ClientApplication
{
    /** Application entry point. */
    public static void main(String[] arguments)
    {
        new Parse().run(arguments);
    }

    /** Runs the application. */
    @Override
    public void coreLogic(String[] arguments) throws ApplicationFailure
    {
        // Ensure that there are exactly two arguments.
        if(arguments.length != 2)
        {
            throw new ApplicationFailure("usage: parse path (hostname | path)");
        }

        // Parse the path. If the path turns out to be remote, print the
        // hostname or path to standard output and exit.
        RemotePath      path;

        try
        {
            path = new RemotePath(arguments[0]);
        }
        catch(IllegalArgumentException e)
        {
            throw new ApplicationFailure("cannot parse path: " +
                                         e.getMessage());
        }

        // Print the requested piece of information.
        boolean         printed = false;

        if(arguments[1].equals("hostname"))
        {
            System.out.print(path.hostname);
            printed = true;
        }

        if(arguments[1].equals("path"))
        {
            System.out.print(path.path);
            printed = true;
        }

        // If a valid request was not made, return with a non-zero exit
        // code.
        if(!printed)
        {
            throw new ApplicationFailure("second argument must be either " +
                                         "hostname or path");
        }
    }
}
