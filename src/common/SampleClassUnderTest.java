package common;

/** Sample class demonstrating unit tests, together with
    <code>SampleUnitTest</code>.

    <p>
    This class and <code>SampleUnitTest</code> should be deleted before handin.

    <p>
    This is a package-private class, not accessible to conformance tests, and
    not part of any public interface. If you have such a class, you can still
    write tests for it, if you wish. The class <code>SampleUnitTest</code>,
    located in <code>unit/common/SampleUnitTest.java</code> in the project
    directory, is able to access this class because it is in the same package
    (<code>common</code>).

    <p>
    Unit tests are a clean way to test complicated non-public classes in your
    packages, without polluting the main source code directories with
    non-production code. The unit tests are in a parallel source tree under the
    directory <code>unit/</code>.
 */
class SampleClassUnderTest
{
    /** Returns the triple of its argument. */
    int triple(int x)
    {
        return x * 3;
    }
}
