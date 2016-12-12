/** Unit tests.

    <p>
    Each unit test resides in the same package as the code it is testing -
    unlike a conformance test, which resides in a separate package. This allows
    the unit test to test package-private classes and methods.

    <p>
    Unit tests must be written to match the internal structure of each version
    of this project. This package is provided for your convenience for you to
    test your own code, if you wish to use the package for this.

    <p>
    To create a new unit test, create a <code>.java</code> file in a
    subdirectory of <code>unit/</code>, and place in it a public class derived
    from {@link test.Test}. For example, if you intend to write a test for locks
    in the naming server, you might create a new file
    <code>unit/naming/LockTest.java</code>. The class in the new file should
    reside in the same package as the class being tested. In the example, the
    first Java directive in the file <code>LockTest.java</code> would be
    <code>package naming</code>.

    <p>
    After creating your test, add its class to the array of tests in the
    <code>main</code> method of <code>unit.UnitTest</code>. To run all unit
    tests, execute <code>java -cp .:unit unit.UnitTest</code> from the command
    line. On a Windows system, execute
    <code>java -cp ".;unit" unit.UnitTests</code> instead.
 */
package unit;
