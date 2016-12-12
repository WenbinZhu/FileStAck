package test;

import java.util.*;

/** Common helper methods for testing. */
public abstract class TestUtil
{
    /** Checks if two arrays contain the same elements.

        @param array1 The first array.
        @param array2 The second array.
        @return <code>true</code> if the two arrays contain the same elements,
                with the same counts, regardless of their order, and
                <code>false</code> otherwise.
     */
    public static <T> boolean sameElements(T[] array1, T[] array2)
    {
        // If the arrays have different sizes, they cannot have the same
        // elements.
        if(array1.length != array2.length)
            return false;

        // Create a multiset - a mapping from element values to appearance
        // counts.
        Map<T, Integer>     multiset = new HashMap<T, Integer>();

        // Insert all the elements in array1 into the multiset.
        for(T element : array1)
        {
            Integer         count = multiset.get(element);

            if(count == null)
                count = 1;
            else
                ++count;

            multiset.put(element, count);
        }

        // Remove all the elements in array2 from the multiset. If an element
        // cannot be removed from the multisets, the arrays are not equal.
        for(T element : array2)
        {
            Integer         count = multiset.get(element);

            if(count == null)
                return false;
            else
            {
                --count;

                if(count == 0)
                    multiset.remove(element);
                else
                    multiset.put(element, count);
            }
        }

        // At this point, if the multiset is empty, then the arrays have the
        // same elements.
        return multiset.isEmpty();
    }
}
