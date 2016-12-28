package apps;

import common.*;

/** Remote path parser.

    <p>
    This path parser is used by all filesystem utility programs. It accepts
    paths in the <em>remote path format</em>. In this format, a full absolute
    path is given as <code>hostname:path</code>, where <code>hostname</code> is
    the hostname of a distributed filesystem naming server, and
    <code>path</code> is an absolute path to a file or directory on that naming
    server.

    <p>
    This permits the user to, say, retrieve a file from the distributed
    filesystem by issuing a command such as
    <pre>
    java -jar dfs.jar get unix.andrew.cmu.edu:/home/notes.txt ./
    </pre>
    assuming the filesystem code is packaged in an archive <code>dfs.jar</code>
    and there is a naming server running at <code>unix.andrew.cmu.edu</code>.

    <p>
    There are several ways to abbreviate remote paths. This class is aware of
    two environment variables, <code>DFSHOST</code> and <code>DFSCWD</code>.
    By setting <code>DFSHOST</code>, it is possible to omit the host entirely.
    Then, a remote path such as <code>192.168.3.3:/path</code> can be written
    simply as <code>:/path</code> or <code>/path</code>.

    <p>
    The remote path given may be either absolute or relative. If the path is
    relative, then it is relative to an absolute path in the <code>DFSCWD</code>
    environment variable. Both absolute and relative paths may include the
    special components <code>.</code> and <code>..</code>, which refer to the
    current and parent directories, respectively. As an example, a path such as
    <code>192.168.3.3:/path/..</code> actually refers to the root directory of
    the filesystem hosted at <code>192.168.3.3</code>. A path such as
    <code>192.168.3.3:index.html</code> refers to <code>index.html</code> in the
    current directory. If the value of <code>DFSCWD</code> is
    <code>/project2-master/javadoc</code>, then the path is referring to
    <code>/project2-master/javadoc/index.html</code> on
    <code>192.168.3.3</code>.

    <p>
    The shortest remote path possible is <code>:</code>, which refers to the
    current directory on the current host. This path can be written in two ways:
    <code>.</code> is the other way.

    <p>
    The environment variables <code>DFSHOST</code> and <code>DFSCWD</code> are
    meant to be set by an external script in response to a <code>cd</code>-like
    command. With a well-written script, they allow relatively easy navigation
    of the distributed filesystem from a shell, without a need to constantly
    type the same hostname and long absolute paths.
 */
class RemotePath
{
    /** Naming server hostname. */
    final String                    hostname;
    /** Path portion of the remote path. */
    final Path                      path;

    /** Name of the environment variable storing the current naming server. Used
        when a remote path is given without a hostname. */
    private static final String     hostname_variable = "DFSHOST";
    /** Name of the environment variable storing the current working directory.
        Used when a relative remote path is given. */
    private static final String     directory_variable = "DFSCWD";

    /** Remote path separator. Used in the remote path parser. */
    private static final String     separator = "/";
    /** Current directory name. Used in the remote path parser. */
    private static final String     current_directory = ".";
    /** Parent directory name. Used in the remote path parser. */
    private static final String     parent_directory = "..";

    /** Parses a remote path string and creates a <code>RemotePath</code>
        object.

        <p>
        If the path string contains a colon character (<code>:</code>),
        everything preceding the colon is taken to be the hostname, and
        everything following the colon is taken to be the distributed filesystem
        path. If there is no colon, then the hostname is taken to be empty, and
        the path is taken to be the entire string.

        @param remote_path The raw path string to be parsed.
        @throws IllegalArgumentException If the path string is malformed.
     */
    RemotePath(String remote_path)
    {
        // Search for the colon in the string.
        int         colon_index = remote_path.indexOf(":");

        String      raw_hostname;
        String      raw_path;

        if(colon_index == -1)
        {
            raw_hostname = "";
            raw_path = remote_path;
        }
        else
        {
            raw_hostname = remote_path.substring(0, colon_index);
            raw_path =
                remote_path.substring(colon_index + 1, remote_path.length());
        }

        hostname = parseHostname(raw_hostname);
        path = parseRemotePath(raw_path);
    }

    /** Creates a remote path object from the hostname of an existing object,
        and a replacement filesystem path.

        <p>
        This constructor is used primarily for pretty-printing paths, to create
        a <code>RemotePath</code> object for access to its <code>toString</code>
        method.

        @param existing_path Existing <code>RemotePath</code> object.
        @param new_path Replacement path portion for the new
                        <code>RemotePath</code>. The hostname of
                        <code>existing_path</code> is used for the new
                        <code>RemotePath</code>.
     */
    private RemotePath(RemotePath existing_path, Path new_path)
    {
        hostname = existing_path.hostname;
        path = new_path;
    }

    /** Parses the hostname portion of a remote path.

        <p>
        If the hostname given with the path is empty, the value of the
        <code>DFSHOST</code> environment variable is used for the hostname. If
        this environment variable is empty or undefined, then no hostname has
        been given, and the path, as given, cannot be parsed.

        @param hostname The hostname portion of the remote path given by the
                        user. This is simply everything preceding the colon, if
                        there is one, or the empty string if there is no colon.
        @return If the user gave a hostname, the hostname is returned.
                Otherwise, this method returns the value of the
                <code>DFSHOST</code> environment variable, if it is set.
        @throws IllegalArgumentException If the user has not specified a host in
                                         the remote path, and the
                                         <code>DFSHOST</code> environment
                                         variable is empty, undefined, or cannot
                                         be accessed.
     */
    private String parseHostname(String hostname)
    {
        if(hostname.length() > 0)
            return hostname;

        try
        {
            hostname = System.getenv(hostname_variable);
        }
        catch(Throwable t)
        {
            throw new IllegalArgumentException("cannot retrieve value of " +
                                               hostname_variable);
        }

        if((hostname == null) || (hostname.length() == 0))
        {
            throw new IllegalArgumentException("no hostname is given and no " +
                                               "current hostname is defined");
        }

        return hostname;
    }

    /** Parses the path portion of a remote path.

        <p>
        If the path portion begins with a forward slash (<code>/</code>), it is
        considered to be an absolute path. Otherwise, it is considered to be a
        path relative to the absolute path stored in the <code>DFSCWD</code>
        environment variable. In the first case, this method assembles a path
        starting from the root directory. In the second case, the path is
        assembled starting from the directory set by the environment variable.
        The special path components <code>.</code> and <code>..</code> are
        interpreted as the current and parent directories, respectively, and
        may occur in both absolute and relative paths. Empty components are
        treated as references to the current directory.

        <p>
        If the <code>DFSCWD</code> environment variable is not set, the default
        value of <code>/</code> is used instead.

        @param raw_path The path portion of the remote path given by the user.
               This is everything following the colon, if there is one, or the
               whole path string, if there is no colon.
        @return A <code>Path</code> object representing the filesystem path
                obtained from parsing the remote path given by the user
                according to the rules described above.
        @throws IllegalArgumentException If the user gave a relative path but
                                         <code>DFSCWD</code> is set and does not
                                         contain an absolute path, or cannot be
                                         accessed, if any path component
                                         contains an illegal character such as
                                         the colon (<code>:</code>), or if the
                                         user has made a reference to the parent
                                         of the root directory, for example by
                                         providing a path such as
                                         <code>/..</code>.
     */
    private Path parseRemotePath(String raw_path)
    {
        // The current path object. This is set initially to either the root
        // directory or the path obtained from parsing the value of DFSCWD, and
        // modified as components are parsed.
        Path            current_path;
        // Index into remote_path from which the next component is to be read.
        int             cursor_position;
        // Index to the end of the component that is to be read.
        int             end_of_component;

        // Check if the path begins with the separator. If so, it is an absolute
        // path. Otherwise, it is relative.
        if(raw_path.indexOf(separator) == 0)
        {
            // If the path is absolute, begin assembling the Path object from
            // the root directory, and set the cursor position to ignore the
            // first separator.
            current_path = new Path();
            cursor_position = 1;
        }
        else
        {
            // If the path is absolute, first retrieve the working directory
            // string from the DFSCWD environment variable.
            String      working_directory;

            try
            {
                working_directory = System.getenv(directory_variable);
            }
            catch(Throwable t)
            {
                throw new IllegalArgumentException("cannot retrieve value of " +
                                                   directory_variable);
            }

            // If DFSCWD is not set, use / as a default value.
            if(working_directory == null)
                working_directory = separator;

            // Attempt to create a base path from the value of DFSCWD.
            try
            {
                current_path = new Path(working_directory);
            }
            catch(IllegalArgumentException e)
            {
                throw new IllegalArgumentException("cannot parse contents of " +
                                                   directory_variable + ": " +
                                                   e.getMessage());
            }

            // Set the cursor position so that the first component of the
            // relative path begins with the first character.
            cursor_position = 0;
        }

        // As long as the cursor position is not moved past the end of the path,
        // search for the end of each component, and adjust the current path
        // according to the component found.
        while(cursor_position < raw_path.length())
        {
            end_of_component = raw_path.indexOf(separator, cursor_position);
            if(end_of_component == -1)
                end_of_component = raw_path.length();

            String      component =
                raw_path.substring(cursor_position, end_of_component);

            // Advance the cursor position past the separator found. If no
            // separator was found, this will move the cursor position past the
            // end of the given path string, terminating the loop after this
            // iteration. This will also terminate the loop if the path happens
            // to end with a separator.
            cursor_position = end_of_component + 1;

            // If the component is empty, it refers to the current directory -
            // do not modify the path.
            if(component.length() == 0)
                continue;

            // If the component is the current directory, do not modify the
            // path.
            if(component.equals(current_directory))
                continue;

            // If the component is the parent directory, check that the current
            // path is not the root directory. If not, get the parent path and
            // make it the new current path.
            if(component.equals(parent_directory))
            {
                if(current_path.isRoot())
                {
                    throw new IllegalArgumentException("path component " +
                                                       "refers to parent of " +
                                                       "root directory");
                }

                current_path = current_path.parent();
                continue;
            }

            // If the component is a regular component, append it to the path
            // and continue to the next iteration (if there will be one).
            current_path = new Path(current_path, component);
        }

        return current_path;
    }

    /** Returns a <code>RemotePath</code> representing the parent directory of
        the given <code>RemotePath</code>.

        <p>
        This is primarily used for pretty-printing parent paths. A new
        <code>RemotePath</code> object is created for access to its
        <code>toString</code> method.

        @throws IllegalArgumentException If the given remote path has no parent.
     */
    RemotePath parent()
    {
        return new RemotePath(this, path.parent());
    }

    /** Converts the path to a string for printing in error messages.

        <p>
        The path is converted to the format <code>hostname:path</code>.
     */
    @Override
    public String toString()
    {
        return hostname + ":" + path;
    }
}
