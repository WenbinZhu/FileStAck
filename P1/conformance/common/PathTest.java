package conformance.common;

import test.*;
import common.*;
import java.io.*;
import java.util.*;

/** Tests the path library.

    <p>
    Tests include:
    <ul>
    <li>The constructors reject empty paths, components, and component strings
        containing the path separator character.</li>
    <li>The constructors create correct paths (checked with
        <code>toString</code> and <code>equals</code>.</li>
    <li>The <code>root</code>, <code>parent</code>, and <code>last</code>
        methods have correct behavior.</li>
    <li>The <code>isSubpath</code> method correctly identifies subpaths.</li>
    <li>The <code>list</code> method lists the files in a directory
        correctly.</li>
    <li>The path iterator correctly iterates over the components of paths, and
        the <code>remove</code> operation is not supported.</li>
    </ul>
 */
public class PathTest extends Test
{
    /** Test notice. */
    public static final String  notice =
        "checking path library public interface";

    /** Performs the tests.

        @throws TestFailed If any of the tests fail.
     */
    @Override
    protected void perform() throws TestFailed
    {
        testConstructors();
        testSplitting();
        testSubpaths();
        testListing();
        testIterator();
    }

    /** Tests the <code>list</code> method.

        <p>
        This test creates a temporary directory with a certain internal
        structure, and then checks that the <code>list</code> method lists the
        files in the directory correctly.

        @throws TestFailed If the test fails.
     */
    private void testListing() throws TestFailed
    {
        TemporaryDirectory  directory = null;

        try
        {
            // Create a new temporary directory.
            directory = new TemporaryDirectory();

            // Add some files to the temporary directory.
            directory.add(new String[] {"file1"});
            directory.add(new String[] {"file2"});
            directory.add(new String[] {"subdirectory", "file3"});
            directory.add(new String[] {"subdirectory", "file4"});

            // List the files in the directory.
            File    file = directory.root();
            Path[]  listed = Path.list(file);

            // Check that the correct files have been listed.
            Path[]  expected = new Path[] {new Path("/file1"),
                                           new Path("/file2"),
                                           new Path("/subdirectory/file3"),
                                           new Path("/subdirectory/file4")};

            if(!TestUtil.sameElements(listed, expected))
                throw new TestFailed("directory listing incorrect");
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("error while testing directory listing", t);
        }
        finally
        {
            if(directory != null)
                directory.remove();
        }
    }

    /** Tests <code>Path</code> constructors and the <code>toString</code> and
        <code>equals</code> methods.

        @throws TestFailed If any of the tests fail.
     */
    private void testConstructors() throws TestFailed
    {
        // Make sure the Path(Path, String) constructor rejects empty
        // components and strings containing the separator or colon.
        try
        {
            new Path(new Path(), "");
            throw new TestFailed("Path(Path, String) constructor accepted " +
                                 "empty string");
        }
        catch(TestFailed e) { throw e; }
        catch(IllegalArgumentException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("Path(Path, String) constructor threw " +
                                 "unexpected exception when given empty " +
                                 "string", t);
        }

        try
        {
            new Path(new Path(), "dir/file");
            throw new TestFailed("Path(Path, String) constructor accepted " +
                                 "string containing the separator");
        }
        catch(TestFailed e) { throw e; }
        catch(IllegalArgumentException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("Path(Path, String) constructor threw " +
                                 "unexpected exception when given string " +
                                 "containing the separator", t);
        }

        try
        {
            new Path(new Path(), "hostname:path");
            throw new TestFailed("Path(Path, String) constructor accepted " +
                                 "string containing a colon");
        }
        catch(TestFailed e) { throw e; }
        catch(IllegalArgumentException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("Path(Path, String) constructor threw " +
                                 "unexpected exception when given string " +
                                 "containing a colon", t);
        }

        // Make sure the Path(String) constructor rejects strings that do not
        // begin with the separator or contain a colon.
        try
        {
            new Path("some-file");
            throw new TestFailed("Path(String) constructor accepted string " +
                                 "that does not start with separator");
        }
        catch(TestFailed e) { throw e; }
        catch(IllegalArgumentException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("Path(String) constructor threw unexpected " +
                                 "exception when given string not strating " +
                                 "with separator", t);
        }

        try
        {
            new Path("hostname:path");
            throw new TestFailed("Path(String) constructor accepted string " +
                                 "containing a colon");
        }
        catch(TestFailed e) { throw e; }
        catch(IllegalArgumentException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("Path(String) constructor threw unexpected " +
                                 "exception when given string containing a " +
                                 "colon", t);
        }

        // Make sure that the constructor will reject the empty string, in
        // particular.
        try
        {
            new Path("");
            throw new TestFailed("Path(String) constructor accepted empty " +
                                 "string");
        }
        catch(TestFailed e) { throw e; }
        catch(IllegalArgumentException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("Path(String) constructor threw unexpected " +
                                 "exception when given empty string", t);
        }

        try
        {
            // Create a root path using the constructor.
            Path    root = new Path();

            if(!root.toString().equals("/"))
            {
                throw new TestFailed("string representation of root path " +
                                     "incorrect");
            }

            // Create a child path of the root.
            Path    child = new Path(root, "subdir");

            if(!child.toString().equals("/subdir"))
                throw new TestFailed("string representation of path incorrect");

            // Create a root path by giving its string representation.
            Path    alternative_root = new Path("/");

            if(!alternative_root.toString().equals("/"))
            {
                throw new TestFailed("string representation of root path " +
                                     "incorrect");
            }

            // Create the same child path by giving its string representation.
            Path    alternative_child = new Path("/subdir");

            if(!alternative_child.toString().equals("/subdir"))
                throw new TestFailed("string representation of path incorrect");

            // Create the same child path by a string representation with many
            // empty components.
            Path    third_child = new Path("///subdir//");

            // Check equality among the objects created.
            if(!third_child.toString().equals("/subdir"))
                throw new TestFailed("string representation of path incorrect");

            if(!root.equals(alternative_root))
                throw new TestFailed("root directories not equal");

            if(!child.equals(alternative_child))
                throw new TestFailed("same children not equal");

            if(!child.equals(third_child))
                throw new TestFailed("same children not equal");

            if(root.equals(child))
                throw new TestFailed("child and root should not be equal");
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("error while testing Path constructors", t);
        }
    }

    /** Tests consistency among the <code>isRoot</code>, <code>parent</code>,
        and <code>last</code> methods.

        @throws TestFailed If any of the tests fail.
     */
    private void testSplitting() throws TestFailed
    {
        try
        {
            // Run tests on the root path.
            Path    root = new Path();

            if(!root.isRoot())
                throw new TestFailed("root path not reported as root");

            try
            {
                root.parent();
                throw new TestFailed("root directory has parent");
            }
            catch(TestFailed e) { throw e; }
            catch(IllegalArgumentException e) { }
            catch(Throwable t)
            {
                throw new TestFailed("unexpected error while checking that " +
                                     "root directory has no parent", t);
            }

            try
            {
                root.last();
                throw new TestFailed("root directory has last component");
            }
            catch(TestFailed e) { throw e; }
            catch(IllegalArgumentException e) { }
            catch(Throwable t)
            {
                throw new TestFailed("unexpected error while checking that " +
                                     "root directory has no last component", t);
            }

            // Run tests on a descendant of the root.
            Path    descendant = new Path("/subdirectory/file");

            if(descendant.isRoot())
                throw new TestFailed("path reported as root");

            try
            {
                if(!descendant.parent().equals(new Path("/subdirectory")))
                    throw new TestFailed("parent directory incorrect");
            }
            catch(TestFailed e) { throw e; }
            catch(Throwable t)
            {
                throw new TestFailed("unexpected error while checking that " +
                                     "path has parent path", t);
            }

            try
            {
                if(!descendant.last().equals("file"))
                    throw new TestFailed("last component incorrect");
            }
            catch(TestFailed e) { throw e; }
            catch(Throwable t)
            {
                throw new TestFailed("unexpected error while checking that " +
                                     "path has last component", t);
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("error while testing path splitting", t);
        }
    }

    /** Tests the <code>isSubpath</code> method.

        @throws TestFailed If the test fails.
     */
    private void testSubpaths() throws TestFailed
    {
        Path        path = new Path("/directory/file");

        if(path.isSubpath(new Path("/directory2")))
        {
            throw new TestFailed("path that is not a prefix reported as " +
                                 "subpath");
        }

        if(!(path.isSubpath(new Path("/directory"))))
            throw new TestFailed("proper prefix is not reported as subpath");

        if(path.isSubpath(new Path("/directory/file/file")))
            throw new TestFailed("longer path reported as subpath");

        if(!(path.isSubpath(new Path("/directory/file"))))
            throw new TestFailed("path not reported as subpath of itself");

        if(!(path.isSubpath(new Path("/"))))
            throw new TestFailed("root not reported as subpath");
    }

    /** Tests the operation of the path iterator.

        @throws TestFailed If any of the tests fail.
     */
    private void testIterator() throws TestFailed
    {
        // Create a path for the test.
        Path                path;

        try
        {
            path = new Path("/usr/bin/java");
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to create path", t);
        }

        // Obtain an iterator for the test.
        Iterator<String>    components = path.iterator();
        String[]            component_array = {"usr", "bin", "java"};
        int                 component_index = 0;

        // Go through each element in the iterator and check it against the
        // expected value.
        while(components.hasNext())
        {
            String          component;

            try
            {
                component = components.next();
            }
            catch(NoSuchElementException e)
            {
                throw new TestFailed("iterator prematurely exhausted", e);
            }

            if(!component.equals(component_array[component_index]))
                throw new TestFailed("iterator returned incorrect component");

            ++component_index;
        }

        // Make sure that the iterator has exhausted the elements in the
        // components array and only those elements.
        if(component_index < component_array.length)
            throw new TestFailed("iterator did not exhaust all components");

        try
        {
            components.next();
            throw new TestFailed("iterator advanced past end of components");
        }
        catch(TestFailed e) { throw e; }
        catch(NoSuchElementException e) { }

        // Make sure the remove operation is reported as not supported.
        try
        {
            components.remove();
            throw new TestFailed("iterator accepted call to remove");
        }
        catch(TestFailed e) { throw e; }
        catch(UnsupportedOperationException e) { }
    }
}
