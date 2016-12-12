package client;

import java.io.*;
import java.net.*;

import rmi.*;
import common.*;
import naming.*;
import storage.*;

/** Input stream backed by a file in the distributed filesystem.

    <p>
    Read calls on a <code>DFSInputStream</code> are directed to a storage server
    hosting the given file. Each read call corresponds to one network request.
    If this behavior is not desirable, the <code>DFSInputStream</code> should be
    wrapped in a <code>BufferedInputStream</code> or other class providing
    buffered input.

    <p>
    <code>DFSInputStream</code> does not support marks.
 */
public class DFSInputStream extends InputStream
{
    /** Path to the file. */
    private final Path      path;
    /** Storage server hosting the file. */
    private final Storage   storage_server;
    /** Naming server used to find the storage server hosting the file. */
    private final Service   naming_server;

    /** Current read offset in the file. */
    private long            offset = 0;
    /** Total file length. */
    private final long      length;

    /** Indicates that the stream has been closed. */
    private boolean         closed = false;

    /** Creates a <code>DFSInputStream</code> for a file listed by the given
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
    public DFSInputStream(Service naming_server, Path file)
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

        // Retrieve the length of the file from the storage server.
        try
        {
            length = storage_server.size(file);
        }
        catch(RMIException e)
        {
            throw new IOException("could not contact storage server", e);
        }

        path = file;
        this.naming_server = naming_server;
    }

    /** Creates a <code>DFSInputStream</code> for a file listed by the given
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
    public DFSInputStream(String hostname, Path file)
        throws FileNotFoundException, IOException
    {
        this(NamingStubs.service(hostname), file);
    }

    /** Creates a <code>DFSInputStream</code> for a file listed by the given
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
    public DFSInputStream(Service naming_server, String filename)
        throws FileNotFoundException, IOException
    {
        this(naming_server, new Path(filename));
    }

    /** Creates a <code>DFSInputStream</code> for a file listed by the given
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
    public DFSInputStream(String hostname, String filename)
        throws FileNotFoundException, IOException
    {
        this(NamingStubs.service(hostname), new Path(filename));
    }

    /** Closes the input stream.

        <p>
        The stream is marked as closed. Further attempts to use the input stream
        will result in <code>IOException</code>.
     */
    @Override
    public void close()
    {
        closed = true;
    }

    /** Reads bytes from the input stream into a byte buffer.

        <p>
        The read is performed in a single request to the storage server. If the
        operation succeeds, the number of bytes read will be either
        <code>read_length</code> or the number of bytes remaining in the file,
        whichever is less.

        @param buffer Buffer to receive bytes read from the stream.
        @param buffer_offset Offset into the buffer at which the bytes are to be
                             written.
        @param read_length The maximum number of bytes to read.
        @return The number of bytes successfully read, or <code>-1</code> if the
                end of file is reached before any bytes are read. The number of
                bytes successfully read may be zero if <code>read_length</code>
                is zero.
        @throws IOException If the stream is closed, if the storage server
                            cannot be contacted, or if a read error occurs on
                            the storage server.
        @throws NullPointerException If <code>buffer</code> is
                                     <code>null</code>.
        @throws IndexOutOfBoundsException If <code>buffer_offset</code> or
                                          <code>read_length</code> is negative,
                                          or if <code>buffer_offset +
                                          read_length</code> exceeds the length
                                          of the given buffer.
     */
    @Override
    public int read(byte[] buffer, int buffer_offset, int read_length)
        throws IOException
    {
        // Stop immediately if the stream is closed.
        if(closed)
        {
            throw new IOException("distributed filesystem input stream " +
                                  "already closed");
        }

        // Check that all the arguments are valid.
        if(buffer == null)
            throw new NullPointerException("buffer array argument is null");

        if(buffer_offset < 0)
            throw new IndexOutOfBoundsException("buffer offset is negative");

        if(read_length < 0)
            throw new IndexOutOfBoundsException("read length is negative");

        if((buffer_offset + read_length) > buffer.length)
            throw new IndexOutOfBoundsException("range extends past buffer");

        // If the request is for zero bytes, return immediately without
        // modifying the buffer or contacting the storage server.
        if(read_length == 0)
            return 0;

        // If the stream offset is at or past the end of file, return -1
        // immediately.
        if(offset >= length)
            return -1;

        // Adjust read_length if it exceeds the number of bytes remaining in the
        // file.
        if(read_length > (length - offset))
            read_length = (int)(length - offset);

        // Read bytes from file and advance the stream offset if the request
        // succeeds.
        byte[]      result;

        try
        {
            result = storage_server.read(path, offset, read_length);
            offset += read_length;
        }
        catch(FileNotFoundException e)
        {
            throw new IOException("file missing on storage server", e);
        }
        catch(RMIException e)
        {
            throw new IOException("unable to contact storage server", e);
        }

        // Copy bytes from the buffer that was received over the network into
        // the buffer provided by the caller.
        for(int index = 0; index < read_length; ++index)
            buffer[buffer_offset + index] = result[index];

        // Return the number of bytes read.
        return read_length;
    }

    /** Reads a single byte from the input stream.

        @return The value of the byte read, as an integer between <code>0</code>
                and <code>255</code>, or <code>-1</code> if the end of file has
                been reached.
        @throws IOException If the stream is closed, if the storage server
                            cannot be contacted, or if a read error occurs on
                            the storage server.
     */
    @Override
    public int read() throws IOException
    {
        // This method relies on the read(byte[], int, int) method. Create a
        // buffer with enough space for one byte, read a byte into that buffer,
        // and return the appropriate result.
        byte[]      buffer = new byte[1];
        int         result = read(buffer, 0, 1);

        if(result == -1)
            return -1;

        return buffer[0];
    }

    /** Advances the stream offset.

        <p>
        The stream offset is advanced by either the given number of bytes, or by
        the number of bytes remaining in the file, whichever is less.

        @param count Number of bytes by which the stream offset should be
                     advanced.
        @return The number of bytes by which the stream offset has actually been
                advanced. This number may be zero.
        @throws IOException If the stream has been closed.
     */
    @Override
    public long skip(long count) throws IOException
    {
        if(closed)
        {
            throw new IOException("distributed filesystem input stream " +
                                  "already closed");
        }

        if(count < 0)
            return 0;

        if(count > (length - offset))
            count = length - offset;

        offset += count;

        return count;
    }

    /** Returns the number of bytes remaining between the current stream offset
        and the end of file.

        <p>
        The number returned is exact and not an estimate. However, if the number
        exceeds <code>Integer.MAX_VALUE</code>, it is clamped to
        <code>Integer.MAX_VALUE</code>.

        @throws IOException If the stream has been closed.
     */
    @Override
    public int available() throws IOException
    {
        if(closed)
        {
            throw new IOException("distributed filesystem input stream " +
                                  "already closed");
        }

        if(length - offset > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;

        return (int)(length - offset);
    }
}
