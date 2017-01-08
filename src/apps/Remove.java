package apps;

import java.util.*;

import common.*;
import naming.*;

/** Removes files and directories in the distributed filesystem.

    <p>
    This application takes a list of remote paths, and attempts to delete the
    files and directories named by those paths. Directory deletion is recursive.
 */
public class Remove extends ClientApplication
{
    /** Application entry point. */
    public static void main(String[] arguments)
    {
        new Remove().run(arguments);
    }

    /** Application main method.

        @param arguments Command line arguments.
     */
    @Override
    public void coreLogic(String[] arguments) throws ApplicationFailure
    {
        // Check that there is exactly one argument.
        if(arguments.length < 1)
            throw new ApplicationFailure("usage: rm path ...");

        // Delete the item named by each argument.
        for(String remote_path : Arrays.asList(arguments))
        {
            try
            {
                remove(remote_path);
                report();
            }
            catch(ApplicationFailure e)
            {
                report(e);
            }
        }
    }

    /** Deletes a single file or directory.

        @param remote_path Path to the file or directory to be deleted.
        @throws ApplicationFailure If the file or directory cannot be deleted.
     */
    private void remove(String remote_path) throws ApplicationFailure
    {
        // Parse the argument.
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

        Service         naming_server = NamingStubs.service(object.hostname);

        // Delete the object in question.
        try
        {
            if(!naming_server.delete(object.path))
                throw new ApplicationFailure(object + " could not be deleted");
        }
        catch(ApplicationFailure e) { throw e; }
        catch(Throwable t)
        {
            throw new ApplicationFailure("cannot delete " + object + ": " +
                                         t.getMessage());
        }
    }
}
