package apps;

/** Prints the current working directory.

    <p>
    The working directory is simply <code>$DFSHOST:$DFSCWD</code>, if these
    variables are both defined. If <code>DFSCWD</code> is not defined, the
    default value used is the root directory (<code>/</code>).
 */
public class PrintWorkingDirectory extends ClientApplication
{
    /** Application entry point. */
    public static void main(String[] arguments)
    {
        new PrintWorkingDirectory().run(arguments);
    }

    /** Runs the application. */
    @Override
    public void coreLogic(String[] arguments) throws ApplicationFailure
    {
        // Ensure that there are no arguments.
        if(arguments.length != 0)
            throw new ApplicationFailure("usage: pwd");

        try
        {
            // Parse the string referring to the current directory on the
            // current host, and print the result in the remote path format.
            System.out.println(new RemotePath(":"));
        }
        catch(IllegalArgumentException e)
        {
            throw new ApplicationFailure("cannot obtain current working " +
                                         "directory: " + e.getMessage());
        }
    }
}
