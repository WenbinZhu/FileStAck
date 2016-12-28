package apps;

/** Stand-in application for the change directory command.

    <p>
    The change directory command must manipulate the shell environment variables
    <code>DFSHOST</code> and <code>DFSCWD</code>, so it cannot be implemented in
    any Java program. The main purpose of this application is to be present in
    the list of applications that can be started by <code>Launcher</code>, and
    therefore to be shown in its usage message. The application can also be run
    to check that the <code>cd</code> command has received the appropriate
    number of arguments.
 */
public class ChangeDirectoryDummy extends ClientApplication
{
    /** Application entry point. */
    public static void main(String[] arguments)
    {
        new ChangeDirectoryDummy().run(arguments);
    }

    /** Runs the application. */
    @Override
    public void coreLogic(String[] arguments) throws ApplicationFailure
    {
        if(arguments.length != 1)
            throw new ApplicationFailure("expected arguments: new-remote-path");
    }
}
