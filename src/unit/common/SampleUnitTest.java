package common;

import test.*;

/** Sample unit test for the class <code>SampleClassUnderTest</code>.

    <p>
    Unit tests, unlike conformance tests, are located in the same package as the
    classes they are testing. This means they are not restricted to testing only
    the public interfaces of the classes in each package - they can also access
    the package-private classes and methods in their package.

    <p>
    Unit tests are isolated from the code they are testing. They are kept under
    the <code>unit/</code> directory.

    <p>
    Delete this class and <code>SampleClassUnderTest</code> before submitting
    your code.
 */
public class SampleUnitTest extends Test
{
    /** Test notice. */
    public static final String  notice = "running sample unit test";

    /** Performs the sample test.

        @throws TestFailed If the test fails.
     */
    @Override
    protected void perform() throws TestFailed
    {
        SampleClassUnderTest    sample = new SampleClassUnderTest();

        if(sample.triple(4) != 3 * 4)
            throw new TestFailed("triple returned incorrect result");
    }
}
