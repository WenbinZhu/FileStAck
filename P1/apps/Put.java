package apps;

import java.io.*;

import common.*;
import naming.*;
import client.*;

/** Uploads a file to the distributed filesystem.

    <p>
    The <code>put</code> command expects two arguments. The first is the source,
    which must be a path to a local file. The second is the destination, which
    must be a path to a remote file or directory.
 */
public class Put extends ClientApplication
{
    /** At most <code>BLOCK_SIZE</code> bytes of data are sent in a single write
        request. */
    private static final int    BLOCK_SIZE = 1024 * 1024;

    /** Application entry point. */
    public static void main(String[] arguments)
    {
        new Put().run(arguments);
    }

    /** Main method.

        @param arguments Command line arguments.
     */
    @Override
    public void coreLogic(String[] arguments) throws ApplicationFailure
    {
        if(arguments.length != 2)
        {
            throw new ApplicationFailure("usage: put source_file " +
                                         "destination_file");
        }

        // Parse the source and destination paths.
        File            source;
        RemotePath      destination;

        source = new File(arguments[0]);

        try
        {
            destination = new RemotePath(arguments[1]);
        }
        catch(IllegalArgumentException e)
        {
            throw new ApplicationFailure("cannot parse destination path: " +
                                         e.getMessage());
        }

        // The source must refer to an existing file.
        if(!source.exists())
            throw new ApplicationFailure("source file does not exist");

        if(source.isDirectory())
        {
            throw new ApplicationFailure("source path refers to a " +
                                         "directory");
        }

        // Obtain a stub for the remote naming server.
        Service         naming_server =
            NamingStubs.service(destination.hostname);

        byte[]              read_buffer;
        InputStream         input_stream = null;
        DFSOutputStream     output_stream = null;

        try
        {
            // Path to receive the new file. This will either be the destination
            // path as provided, or if the path refers to a directory, then a
            // new file within that directory.
            Path            destination_path = destination.path;

            // If the destination path refers to a directory, create a new path
            // to point to a new file in that directory.
            try
            {
                if(naming_server.isDirectory(destination.path))
                {
                    String  filename = source.getName();
                    if(filename.length() == 0)
                        throw new ApplicationFailure("source filename empty");

                    destination_path = new Path(destination.path, filename);

                    // If, after modifying the path, the path still refers to a
                    // directory, then the application must terminate.
                    if(naming_server.isDirectory(destination_path))
                    {
                        throw new ApplicationFailure(destination + " is a " +
                                                     "directory");
                    }
                }

                // If control has reached here, the destination path exists, and
                // it is a regular file. Attempt to delete it.
                naming_server.delete(destination_path);
            }
            catch(FileNotFoundException e) { }

            // Create a new file with the name of the destination file.
            naming_server.createFile(destination_path);

            // Obtain the size of the source file.
            long            bytes_remaining = source.length();

            // Allocate the temporary read buffer and open streams.
            read_buffer = new byte[BLOCK_SIZE];
            input_stream = new FileInputStream(source);
            output_stream =
                new DFSOutputStream(naming_server, destination_path);

            // As long as there are bytes remaining to be copied from the
            // source file, copy at most BLOCK_SIZE bytes at a time. Read
            // the bytes to be transferred in a single block into the buffer
            // read_buffer, and then send the buffer to the storage server.
            while(bytes_remaining > 0)
            {
                int         bytes_to_transfer = BLOCK_SIZE;
                if(bytes_remaining < BLOCK_SIZE)
                    bytes_to_transfer = (int)bytes_remaining;

                int         bytes_loaded = 0;
                int         bytes_read;

                // Reading from the local file is done in a loop, because
                // the read call for local files does not guarantee that the
                // number of bytes read will be equal to the number of bytes
                // requested, even if care has been taken to avoid premature
                // end of file conditions.
                while(bytes_loaded < bytes_to_transfer)
                {
                    bytes_read =
                        input_stream.read(read_buffer, bytes_loaded,
                                          bytes_to_transfer - bytes_loaded);

                    if(bytes_read <= 0)
                        throw new EOFException("unexpected end of file");

                    bytes_loaded += bytes_read;
                }

                output_stream.write(read_buffer, 0, bytes_to_transfer);
                bytes_remaining -= bytes_to_transfer;
            }
        }
        catch(ApplicationFailure e) { throw e; }
        catch(Throwable t)
        {
            throw new ApplicationFailure("cannot transfer " + source +
                                         ": " + t.getMessage());
        }
        finally
        {
            // In all cases, make an effort to close all streams.
            if(output_stream != null)
            {
                try
                {
                    output_stream.close();
                }
                catch(Throwable t) { }
            }

            if(input_stream != null)
            {
                try
                {
                    input_stream.close();
                }
                catch(Throwable t) { }
            }
        }
    }
}
