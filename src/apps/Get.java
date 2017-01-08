package apps;

import java.io.*;

import common.*;
import naming.*;
import client.*;

/** Retrieves a file stored on the distributed filesystem.

    <p>
    The <code>get</code> command expects two arguments. The first is the source,
    which must be a full path to a remote file. The second is the destination,
    which must be a path to a local file or directory. If the application is
    able to contact the remote server and create the local file, the source file
    is copied to the destination file. If the destination is a directory, a new
    file is created in the directory with the same name as the source file.
 */
public class Get extends ClientApplication
{
    /** The size of each request for data cannot exceed
        <code>BLOCK_SIZE</code>. */
    private static final int    BLOCK_SIZE = 1024 * 1024;

    /** Application entry point. */
    public static void main(String[] arguments)
    {
        new Get().run(arguments);
    }

    /** Main method.

        @param arguments Command line arguments.
     */
    @Override
    public void coreLogic(String[] arguments) throws ApplicationFailure
    {
        if(arguments.length != 2)
        {
            throw new ApplicationFailure("usage: get source_file " +
                                         "destination_file");
        }

        // Parse the source and destination paths.
        RemotePath      source;
        File            destination;

        try
        {
            source = new RemotePath(arguments[0]);
        }
        catch(IllegalArgumentException e)
        {
            throw new ApplicationFailure("cannot parse source path: " +
                                         e.getMessage());
        }

        destination = new File(arguments[1]);

        // If the destination file is a directory, get a path to a file in that
        // directory with the same name as the source file.
        if(destination.isDirectory())
            destination = new File(destination, source.path.last());

        // Get a stub for the naming server.
        Service         naming_server = NamingStubs.service(source.hostname);

        // Create an input stream reading bytes from the remote file, and an
        // output stream for writing bytes to a local copy of the file.
        // Repeatedly read up to BLOCK_SIZE bytes from the remote file, and
        // write them to the local file.
        byte[]              read_buffer;
        DFSInputStream      input_stream = null;
        OutputStream        output_stream = null;

        try
        {
            read_buffer = new byte[BLOCK_SIZE];
            output_stream = new FileOutputStream(destination);
            input_stream = new DFSInputStream(naming_server, source.path);

            int             bytes_remaining = input_stream.available();
            int             bytes_to_transfer;
            int             bytes_read;

            while(bytes_remaining > 0)
            {
                bytes_to_transfer = bytes_remaining;
                if(bytes_to_transfer > BLOCK_SIZE)
                    bytes_to_transfer = BLOCK_SIZE;

                bytes_read =
                    input_stream.read(read_buffer, 0, bytes_to_transfer);
                bytes_remaining = input_stream.available();

                if(bytes_read <= 0)
                    throw new EOFException("unexpected end of file");

                // Write only as many bytes as were actually read.
                output_stream.write(read_buffer, 0, bytes_read);
            }
        }
        catch(Throwable t)
        {
            throw new ApplicationFailure("cannot transfer " + source + ": " +
                                         t.getMessage());
        }
        finally
        {
            // In all cases, make an effort to close all streams.
            if(input_stream != null)
            {
                try
                {
                    input_stream.close();
                }
                catch(Throwable t) { }
            }

            if(output_stream != null)
            {
                try
                {
                    output_stream.close();
                }
                catch(Throwable t) { }
            }
        }
    }
}
