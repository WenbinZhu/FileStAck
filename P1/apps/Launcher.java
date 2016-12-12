package apps;

import java.util.*;

/** Application launcher.

    <p>
    This class is provided as an option in case the filesystem is packaged as
    a single JAR file. In this case, this class provides a single entry point
    and a convenient way to run any of the utilities provided for accessing the
    filesystem.
 */
public class Launcher
{
    /** Exit status indicating failure. */
    public static final int                 EXIT_FAILURE = 2;

    /** Map from application names to application objects for individual
        applications. */
    private static Map<String, Application> applications;

    /** Main entry point.

        @param arguments Command line arguments. The first argument should be
                         the name of the application to be run. Following
                         arguments are passed to the application.
     */
    public static void main(String[] arguments)
    {
        // Create and fill the map from application names to application
        // objects.
        applications = new HashMap<String, Application>();

        applications.put("naming", new NamingServerApp());
        applications.put("storage", new StorageServerApp());

        // Check that at least an application name is present. If not, print a
        // help message and exit.
        if(arguments.length < 1)
            usage();

        // Find the application to be run. If there is no application with the
        // given name, print a help message and exit.
        Application                 application =
            applications.get(arguments[0]);

        if(application == null)
            usage();

        // Create the array of application arguments - these are all the
        // arguments passed to this program, except the application name itself.
        String[]                    application_arguments =
            new String[arguments.length - 1];

        for(int index = 0; index < application_arguments.length; ++index)
            application_arguments[index] = arguments[index + 1];

        // Run the application.
        application.run(application_arguments);
    }

    /** Prints a help message and terminates the application. */
    private static void usage()
    {
        // Display a sorted list of application names.
        System.out.println("first argument must be one of:");

        Set<String>     name_set = applications.keySet();
        String[]        names = new String[name_set.size()];

        name_set.toArray(names);
        Arrays.sort(names);

        for(int index = 0; index < names.length; ++index)
            System.out.println("  " + names[index]);

        // Display additional help information and exit.
        System.out.println("\nfor example, the arguments to start a storage " +
                           "server:");
        System.out.println("  storage 127.0.0.1 127.0.0.1 storage-test/");
        System.out.println("\npaths can take two forms:");
        System.out.println("  naming-server:remote-path    OR    local-path");

        System.exit(EXIT_FAILURE);
    }
}
