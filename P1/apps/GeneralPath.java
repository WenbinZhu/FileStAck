package apps;

import java.io.*;

import common.*;

/** Path parser.

    <p>
    This path parser is used by all filesystem utility programs. It accepts
    paths in two formats. The first is <code>hostname:remote-path</code> is a
    remote filesystem path, where <code>hostname</code> gives the hostname of a
    distributed filesystem naming server, and <code>remote-path</code> is an
    absolute path to a file or directory on that naming server. The second is
    <code>local-path</code>, which is a regular path on the local system,
    following normal system conventions. The local path need not be relative.

    <p>
    This permits the user to, say, copy a file from the distributed filesystem
    by issuing a command such as
    <pre>
    java -jar dfs.jar cp unix.andrew.cmu.edu:/home/notes.txt ./
    </pre>
    assuming the filesystem code is packaged in an archive <code>dfs.jar</code>
    and there is a naming server running at <code>unix.andrew.cmu.edu</code>.

    <p>
    An exception to the above rule for path formatting is made in order to
    handle Windows paths. A path of the form <code>x:path</code>, where
    <code>x</code> is a single letter, is treated as a local path with drive
    letter <code>x</code>, instead of a remote path with hostname
    <code>x</code>.
 */
class GeneralPath
{
    /** Indicates that the path is a local path. */
    final boolean       local;
    /** Local path if the path is local, or <code>null</code> otherwise. */
    final File          local_path;
    /** Remote path if the path is remote, or <code>null</code> otherwise. */
    final Path          remote_path;
    /** Naming server hostname if the path is remote, or <code>null</code>
        otherwise. */
    final String        hostname;

    /** Parses a path string and creates a <code>GeneralPath</code> object.

        <p>
        If the path string contains a colon character (<code>:</code>), the path
        is considered to be a remote path. The substring preceding the colon is
        the naming server hostname, and the substring following the colon is the
        remote path. Otherwise, the whole string is considered to be a local
        path.

        <p>
        An exception is made in case the hostname is one character long. In this
        case, the hostname is assumed to instead be a Windows drive letter,
        making the path a local one.

        @param raw_path The raw path string to be parsed.
        @throws IllegalArgumentException If the path is remote and the path
                                         string is malformed.
     */
    GeneralPath(String raw_path)
    {
        // Search for the colon in the string.
        int     colon_index = raw_path.indexOf(":");

        if((colon_index == -1) || (colon_index == 1))
        {
            // If there is no colon in the string, the path is a local path.
            // Alternatively, if the colon is the second character in the
            // string, this parser guesses that this is a local path that begins
            // with a Windows drive letter.
            local = true;

            local_path = new File(raw_path);

            remote_path = null;
            hostname = null;
        }
        else
        {
            // Otherwise, the path is a remote path.
            local = false;

            local_path = null;

            remote_path = new Path(raw_path.substring(colon_index + 1,
                                                      raw_path.length()));
            hostname = raw_path.substring(0, colon_index);
        }
    }

    /** Converts the path to a string for printing in error messages.

        <p>
        If the path is local, it is converted according to the conventions for
        local paths. If the path is a distributed filesystem path, the path is
        converted to the format <code>"(remote-path) on (hostname)"</code>.
     */
    @Override
    public String toString()
    {
        if(local)
            return local_path.toString();
        else
            return remote_path + " on " + hostname;
    }
}
