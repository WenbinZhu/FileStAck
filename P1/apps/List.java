package apps;

import java.util.*;

import naming.*;

/** Lists files and directories.

    <p>
    This application lists the remote files or directories named by its
    arguments. For each file, it lists the filename. For each directory, it
    lists the contents of the directory. Running the application with no
    arguments is equivalent to listing the current directory on the current host
    - that is, to giving it the single argument <code>:</code>.

    <p>
    Unlike the usual <code>ls</code> command, this application is not capable of
    printing file permissions, owner, group, or modification, access, or
    creation times - because none of these are stored by the filesystem.
 */
public class List extends ClientApplication
{
    /** Application entry point. */
    public static void main(String[] arguments)
    {
        new List().run(arguments);
    }

    /** Application main method.

        @param arguments Command line arguments.
     */
    @Override
    public void coreLogic(String[] arguments) throws ApplicationFailure
    {
        // Check that there is exactly one argument on the command line.
        if(arguments.length == 0)
            arguments = new String[] {":"};

        // Go through all the arguments and list each one.
        for(String remote_path : Arrays.asList(arguments))
        {
            try
            {
                list(remote_path, arguments.length > 1);
                report();
            }
            catch(ApplicationFailure e)
            {
                report(e);
            }
        }
    }

    /** Lists a remote path.

        <p>
        This method is called for each command line argument.

        @param remote_path Path to be listed.
        @param show_path Set to <code>true</code> if the path should be shown
                         before the contents are listed. This is set when there
                         is more than one command line argument, and the output
                         resulting from listing different arguments needs to be
                         distinguished.
        @throws ApplicationFailure If the path cannot be listed
     */
    private void list(String remote_path, boolean show_path)
        throws ApplicationFailure
    {
        // Parse the single argument.
        RemotePath      object;

        try
        {
            object = new RemotePath(remote_path);
        }
        catch(IllegalArgumentException e)
        {
            throw new ApplicationFailure("cannot parse path: " +
                                         e.getMessage());
        }

        String[]        components;

        // If the path is remote, obtain a naming server stub. List the path.
        Service         naming_server = NamingStubs.service(object.hostname);

        try
        {
            if(naming_server.isDirectory(object.path))
                components = naming_server.list(object.path);
            else
                components = new String[] {object.path.last()};
        }
        catch(Throwable t)
        {
            throw new ApplicationFailure("cannot list " + object + ": " +
                                         t.getMessage());
        }

        // Sort the array of components that is returned by list and print it.
        Arrays.sort(components);

        if(show_path)
            System.out.println(remote_path + ":");

        for(int index = 0; index < components.length; ++index)
        {
            if(show_path)
                System.out.print("\t");

            System.out.println(components[index]);
        }
    }
}
