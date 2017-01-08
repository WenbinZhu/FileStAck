package client;

import java.io.*;
import java.net.*;

import rmi.*;
import common.*;
import naming.*;
import storage.*;

/** Output stream directed to a file in the distributed filesystem.

    <p>
    Write calls on a <code>DFSOutputStream</code> are directed to a storage
    server hosting the given file. Each call corresponds to one network request.
    If this is not desirable, the <code>DFSOutputStream</code> should be wrapped
    in a <code>BufferedOutputStream</code> object.

    <p>
    Creating a <code>DFSOutputStream</code> for a file does not cause the file
    to be created or truncated. The file must exist, and the existing file data
    is left in place. Writes to the stream cause file data to be overwritten,
    starting from the beginning of the file.
 */
public class DFSOutputStream extends OutputStream
{
    /** Path to the file. */
    private final Path      path;
    /** Storage server hosting the file. */
    private final Storage   storage_server;
    /** Naming server used to find the storage server hosting the file. */
    private final Service   naming_server;

    /** Current write offset in the file. */
    private long            offset = 0;

    /** Indicates that the stream has been closed. */
    private boolean         closed = false;

    /** Creates a <code>DFSOutputStream</code> for a file listed by the given
        naming server.

        @param naming_server Stub for the naming server hosting metadata for the
                             file.
        @param file Path to the file.
        @throws FileNotFoundException If the file is not listed by the given
                                      naming server, or if the path refers to a
                                      directory.
        @throws IOException If either the naming server or the storage server
                            hosting the file cannot be contacted to retrieve
                            file metadata.
     */
    public DFSOutputStream(Service naming_server, Path file)
        throws FileNotFoundException, IOException
    {
        // Retrieve a stub for the storage server hosting the file.
        try
        {
            storage_server = naming_server.getStorage(file);
        }
        catch(RMIException e)
        {
            throw new IOException("could not contact naming server", e);
        }

        path = file;
        this.naming_server = naming_server;
    }

    /** Creates a <code>DFSOutputStream</code> for a file listed by the given
        naming server.

        <p>
        The naming server is contacted on the default client interface port.

        @param hostname Address of the naming server hosting metadata for the
                        file.
        @param file Path to the file.
        @throws FileNotFoundException If the file is not listed by the given
                                      naming server, or if the path refers to a
                                      directory.
        @throws IOException If either the naming server or the storage server
                            hosting the file cannot be contacted to retrieve
                            file metadata.
     */
    public DFSOutputStream(String hostname, Path file)
        throws FileNotFoundException, IOException
    {
        this(NamingStubs.service(hostname), file);
    }

    /** Creates a <code>DFSOutputStream</code> for a file listed by the given
        naming server.

        @param naming_server Stub for the naming server hosting metadata for the
                             file.
        @param filename Path to the file, as a string.
        @throws FileNotFoundException If the file is not listed by the given
                                      naming server, or if the path refers to a
                                      directory.
        @throws IOException If either the naming server or the storage server
                            hosting the file cannot be contacted to retrieve
                            file metadata.
     */
    public DFSOutputStream(Service naming_server, String filename)
        throws FileNotFoundException, IOException
    {
        this(naming_server, new Path(filename));
    }

    /** Creates a <code>DFSOutputStream</code> for a file listed by the given
        naming server.

        <p>
        The naming server is contacted on the default client interface port.

        @param hostname Address of the naming server hosting metadata for the
                        file.
        @param filename Path to the file, as a string.
        @throws FileNotFoundException If the file is not listed by the given
                                      naming server, or if the path refers to a
                                      directory.
        @throws IOException If either the naming server or the storage server
                            hosting the file cannot be contacted to retrieve
                            file metadata.
     */
    public DFSOutputStream(String hostname, String filename)
        throws FileNotFoundException, IOException
    {
        this(NamingStubs.service(hostname), new Path(filename));
    }

    /** Closes the output stream.

        <p>
        The stream is marked as closed. Further attempts to use the output
        stream will result in <code>IOException</code>.
     */
    @Override
    public void close() throws IOException
    {
        closed = true;
    }

    /** Writes bytes from a buffer to the output stream.

        <p>
        The write is performed in a single request to the storage server.

        @param buffer Buffer containing bytes to be written.
        @param buffer_offset Offset into the buffer from which bytes are to be
                             written.
        @param write_length Number of bytes to be written.
        @throws IOException If the stream is closed, if the storage server
                            cannot be contacted, or if a write error occurs on
                            the storage server.
        @throws NullPointerException If <code>buffer</code> is
                                     <code>null</code>.
        @throws IndexOutOfBoundsException If <code>buffer_offset</code> or
                                          <code>write_length</code> is negative,
                                          or if <code>buffer_offset +
                                          write_length</code> exceeds the length
                                          of the given buffer.
     */
    @Override
    public void write(byte[] buffer, int buffer_offset, int write_length)
        throws IOException
    {
        // Stop immediately if the stream is closed.
        if(closed)
        {
            throw new IOException("distributed filesystem output stream " +
                                  "already closed");
        }

        // Check that all the arguments are valid.
        if(buffer == null)
            throw new NullPointerException("buffer array argument is null");

        if(buffer_offset < 0)
            throw new IndexOutOfBoundsException("buffer offset is negative");

        if(write_length < 0)
            throw new IndexOutOfBoundsException("write length is negative");

        if((buffer_offset + write_length) > buffer.length)
            throw new IndexOutOfBoundsException("range extends past buffer");

        // If the request is to write zero bytes, return immediately without
        // contacting the storage server.
        if(write_length == 0)
            return;

        // Create the data buffer that will be sent over the network. If the
        // buffer offset is zero and all the bytes in the user-provided buffer
        // are to be written, the user-provided buffer will be serialized
        // directly. Otherwise, make a new buffer, copy the requisite number of
        // bytes from the proper offset in the user-provided buffer to the new
        // buffer, and later serialize the new buffer.
        byte[]      data;

        if((buffer_offset == 0) && (buffer.length == write_length))
            data = buffer;
        else
        {
            data = new byte[write_length];

            for(int index = 0; index < write_length; ++index)
                data[index] = buffer[buffer_offset + index];
        }

        // Send the write request to the server. If the write request succeds,
        // advance the stream offset.
        try
        {
            storage_server.write(path, offset, data);
            offset += write_length;
        }
        catch(FileNotFoundException e)
        {
            throw new IOException("file missing on storage server", e);
        }
        catch(RMIException e)
        {
            throw new IOException("unable to contact storage server", e);
        }
    }

    /** Writes a single byte to the output stream.

        @param b Value of the byte to be written. The argument is taken modulo
                 <code>256</code> to obtain the byte value.
        @throws IOException If the stream is closed, if the storage server
                            cannot be contacted, or if a write error occurs on
                            the storage server.
     */
    @Override
    public void write(int b) throws IOException
    {
        write(new byte[] {(byte)b}, 0, 1);
    }

    /** Advances the stream offset.

        <p>
        It is possible to advance the offset past the current end of file.

        @param count Number of bytes to advance stream offset by.
        @throws IOException If the stream has been closed.
     */
    public void skip(long count) throws IOException
    {
        if(closed)
        {
            throw new IOException("distributed filesystem output stream " +
                                  "already closed");
        }

        if(count < 0)
            return;

        offset += count;
    }
}
