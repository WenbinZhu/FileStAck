package conformance.storage;

import test.*;
import common.*;
import java.io.*;
import java.util.*;

/** Tests storage server file access methods.

    <p>
    This test starts a storage server and a special testing naming server. It
    then obtains a stub for the storage server and checks several properties of
    the <code>read</code>, <code>write</code>, and <code>size</code> methods.

    <p>
    Properties checked are:
    <ul>
    <li><code>read</code>, <code>write</code>, and <code>size</code> have
        correct behavior for non-existent files and directories.</li>
    <li><code>read</code>, <code>write</code>, and <code>size</code> respond
        correctly to <code>null</code> arguments.</li>
    <li><code>read</code> and <code>write</code> respond correctly to
        out-of-bounds arguments.</li>
    <li><code>read</code>, <code>write</code>, and <code>size</code> have
        correct behavior when given valid arguments.</li>
    <li><code>write</code> performs random access on files.</li>
    </ul>

    <p>
    The effects of <code>write</code> are checked directly by reading the file
    locally, and by reading it through the storage server stub.
 */
public class AccessTest extends StorageTest
{
    /** Test notice. */
    public static final String  notice =
        "checking storage server file access methods (size, read, write)";
    /** Prerequisites. */
    public static final Class[] prerequisites =
        new Class[] {RegistrationTest.class};

    /** File that is not present on the storage server. */
    private final Path          absent_file = new Path("/absent");
    /** Path to a directory on the storage server. */
    private final Path          directory_file = new Path("/subdirectory");
    /** An empty file on the storage server. */
    private final Path          empty_file = new Path("/subdirectory/file2");
    /** File on the storage server to be used for reading and writing tests. */
    private final Path          read_write_file =
                                            new Path("/subdirectory/file1");

    /** Small buffer of data to be used in writing tests. */
    private final byte[]        write_data = "test data".getBytes();

    /** Creates the <code>AccessTest</code> object. */
    public AccessTest()
    {
        super(new String[][] {new String[] {"subdirectory", "file1"},
                              new String[] {"subdirectory", "file2"},
                              new String[] {"file3"},
                              new String[] {"subdirectory", "subdirectory2",
                                            "file1"}},
              null);
    }

    /** Tests the server file access methods.

        @throws TestFailed If any of the tests fail.
     */
    @Override
    protected void perform() throws TestFailed
    {
        testSize();
        testReadBasic();
        testWriteBasic();
        testReadWrite();
        testReadWriteBounds();
        testAppend();
    }

    /** Tests the <code>write</code> method with valid arguments.

        @throws TestFailed If the test fails.
     */
    private void testReadWrite() throws TestFailed
    {
        // Write test data to file. Check that the data is present in the file
        // by accessing it directly in the server's temporary directory, and by
        // asking the server to retrieve the data. Check also that the file size
        // reported is correct.
        try
        {
            client_stub.write(read_write_file, 0, write_data);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to write to file", t);
        }

        // Check the file locally. First check the file's presence, kind, and
        // size.
        File            direct_access =
            new File(directory.root(), "subdirectory");
        direct_access = new File(direct_access, "file1");

        if(!direct_access.exists())
            throw new TestFailed("file does not exist after writing");

        if(direct_access.isDirectory())
            throw new TestFailed("file is a directory after writing");

        if(direct_access.length() != write_data.length)
            throw new TestFailed("file has incorrect size after writing");

        // Read the file directly from the local fileystem and check its
        // contents.
        FileInputStream stream;

        try
        {
            stream = new FileInputStream(direct_access);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to open written file directly for " +
                                 "reading", t);
        }

        byte[]          local_data;

        try
        {
            local_data = new byte[write_data.length];

            if(stream.read(local_data, 0, write_data.length) !=
                write_data.length)
            {
                throw new TestFailed("end of file reached while reading " +
                                     "written data directly");
            }
        }
        catch(IOException e)
        {
            throw new TestFailed("unable to read written file directly", e);
        }
        finally
        {
            // No matter the circumstances, attempt to close the input stream.
            try
            {
                stream.close();
            }
            catch(Throwable t) { }
        }

        if(!Arrays.equals(local_data, write_data))
        {
            throw new TestFailed("data retrieved from written file directly " +
                                 "do not match written data");
        }

        // Check the file's size and contents through the storage server stub.
        try
        {
            if(client_stub.size(read_write_file) != write_data.length)
            {
                throw new TestFailed("stub reports incorrect file size after " +
                                     "data is written to file");
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("unable to retrieve written file size " +
                                 "through stub", t);
        }

        try
        {
            byte[]      remote_data =
                client_stub.read(read_write_file, 0, write_data.length);

            if(!Arrays.equals(remote_data, write_data))
            {
                throw new TestFailed("data retrieved through stub from " +
                                     "written file does not match written " +
                                     "data");
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("unable to retrieve written file contents " +
                                 "through stub", t);
        }
    }

    /** Tests <code>read</code> and <code>write</code> with arguments that are
        out of file bounds.

        @throws TestFailed If the test fails.
     */
    private void testReadWriteBounds() throws TestFailed
    {
        // Try to perform several reads that are not within the bounds of
        // read_write_file.
        try
        {
            client_stub.read(read_write_file, -1, write_data.length + 1);
            throw new TestFailed("read method allowed negative offset");
        }
        catch(TestFailed e) { throw e; }
        catch(IndexOutOfBoundsException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("read method raised unexpected exception " +
                                 "when reading from negative offset", t);
        }

        try
        {
            client_stub.read(read_write_file, 0, write_data.length + 1);
            throw new TestFailed("read method allowed reading past end of " +
                                 "file");
        }
        catch(TestFailed e) { throw e; }
        catch(IndexOutOfBoundsException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("read method raised unexpected exception " +
                                 "when reading past end of file", t);
        }

        try
        {
            client_stub.read(read_write_file, write_data.length,
                             write_data.length);
            throw new TestFailed("read method allowed offset outside of file");
        }
        catch(TestFailed e) { throw e; }
        catch(IndexOutOfBoundsException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("read method raised unexpected exception " +
                                 "when offset is outside of file", t);
        }

        // Try to perform a read with negative length.
        try
        {
            client_stub.read(read_write_file, 0, -write_data.length);
            throw new TestFailed("read method allowed read with negative " +
                                 "length");
        }
        catch(TestFailed e) { throw e; }
        catch(IndexOutOfBoundsException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("read method raised unexpected exception " +
                                 "when given negative length", t);
        }

        // Try to perform a write with a negative offset.
        try
        {
            client_stub.write(read_write_file, -1, write_data);
            throw new TestFailed("write method allowed write with negative " +
                                 "offset");
        }
        catch(TestFailed e) { throw e; }
        catch(IndexOutOfBoundsException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("write method raised unexpected exception " +
                                 "when given negative offset", t);
        }
    }

    /** Tests <code>write</code> random access capability.

        @throws TestFailed If the test fails.
     */
    private void testAppend() throws TestFailed
    {
        // Write data outside the current bounds of read_write_file and check
        // the size of the resulting file.
        try
        {
            client_stub.write(read_write_file, write_data.length + 1,
                              write_data);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to append data to file", t);
        }

        // Check that the file has a correct new size.
        long    size;

        try
        {
            size = client_stub.size(read_write_file);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to retrieve file size after " +
                                 "appending", t);
        }

        if(size != write_data.length + 1 + write_data.length)
            throw new TestFailed("file has incorrect size after appending");
    }

    /** Tests the <code>size</code> method with bad arguments.

        @throws TestFailed If the test fails.
     */
    private void testSize() throws TestFailed
    {
        // Try to get the size of an absent file.
        try
        {
            client_stub.size(absent_file);
            throw new TestFailed("size method returned for non-existent file");
        }
        catch(FileNotFoundException e) { }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("size method threw unexpected exception " +
                                 "when accessing non-existent file", t);
        }

        // Try to get the size of a directory.
        try
        {
            client_stub.size(directory_file);
            throw new TestFailed("size method returned for directory");
        }
        catch(FileNotFoundException e) { }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("size method threw unexpected exception " +
                                 "when accessing directory", t);
        }

        // Try to get the size of an empty file.
        try
        {
            if(client_stub.size(empty_file) != 0)
            {
                throw new TestFailed("size method returned nonzero result " +
                                     "for empty file");
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("size method threw unexpected exception " +
                                 "when accessing empty file", t);
        }

        // Try to call size with null as argument.
        try
        {
            client_stub.size(null);
            throw new TestFailed("size method returned when given null as " +
                                 "argument");
        }
        catch(NullPointerException e) { }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("size method threw unexpected exception " +
                                 "when given null as argument", t);
        }
    }

    /** Tests the <code>read</code> method with bad arguments.

        @throws TestFailed If the test fails.
     */
    private void testReadBasic() throws TestFailed
    {
        // Try to read from a non-existent file.
        try
        {
            client_stub.read(absent_file, 0, 0);
            throw new TestFailed("read method returned for non-existent file");
        }
        catch(FileNotFoundException e) { }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("read method threw unexpected exception " +
                                 "when accessing non-existent file", t);
        }

        // Try to read from a directory.
        try
        {
            client_stub.read(directory_file, 0, 0);
            throw new TestFailed("read method returned for directory");
        }
        catch(FileNotFoundException e) { }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("read method threw unexpected exception " +
                                 "when accessing directory", t);
        }

        // Try to read from an empty file.
        try
        {
            byte[]  result = client_stub.read(empty_file, 0, 0);

            if(result == null)
            {
                throw new TestFailed("read method returned null when reading " +
                                     "from empty file");
            }

            if(result.length != 0)
            {
                throw new TestFailed("read method returned incorrect number " +
                                     "of bytes when reading empty file");
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("read method threw unexpected exception " +
                                 "when reading from empty file", t);
        }

        // Call read with null as the file argument.
        try
        {
            client_stub.read(null, 0, 0);
            throw new TestFailed("read method returned when given null as " +
                                 "argument");
        }
        catch(NullPointerException e) { }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("read method threw unexpected exception " +
                                 "when given null as argument", t);
        }
    }

    /** Tests the <code>write</code> method with bad arguments.

        @throws TestFailed If the test fails.
     */
    private void testWriteBasic() throws TestFailed
    {
        // Try to write to a non-existent file.
        try
        {
            client_stub.write(absent_file, 0, write_data);
            throw new TestFailed("write method returned for non-existent file");
        }
        catch(FileNotFoundException e) { }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("write method threw unexpected exception " +
                                 "when accessing non-existent file", t);
        }

        // Try to write to a directory.
        try
        {
            client_stub.write(directory_file, 0, write_data);
            throw new TestFailed("write method returned for directory");
        }
        catch(FileNotFoundException e) { }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("write method threw unexpected exception " +
                                 "when accessing directory", t);
        }

        // Try to call write with null as the path.
        try
        {
            client_stub.write(null, 0, write_data);
            throw new TestFailed("write method returned when given null for " +
                                 "path argument");
        }
        catch(NullPointerException e) { }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("write method threw unexpected exception " +
                                 "when given null for path argument", t);
        }

        // Try to call write with null as the data.
        try
        {
            client_stub.write(empty_file, 0, null);
            throw new TestFailed("write method returned when given null for " +
                                 "data argument");
        }
        catch(NullPointerException e) { }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("write method threw unexpected exception " +
                                 "when given null for data argument", t);
        }
    }
}
