package com.stripe.android.testharness;

import android.support.annotation.Nullable;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Utility class for testing with JSON objects and maps that are created from or for JSON.
 */
public class JsonTestUtils {
    /**
     * Assert that two JSON-style {@link Map Maps} are equal, comparing key by key recursively.
     *
     * @param first the first map
     * @param second the second map
     */
    public static void assertMapEquals(@Nullable Map<String, ?> first,
                                       @Nullable Map<String, ?> second) {
        assertSameNullity(first, second);
        if (first == null || second == null) {
            return;
        }

        assertEquals(first.size(), second.size());
        for (String key : first.keySet()) {
            assertTrue(second.containsKey(key));
            Object firstObject = first.get(key);
            Object secondObject = second.get(key);
            assertMapValuesEqual(firstObject, secondObject);
        }
    }

    /**
     * Assert two {@link List Lists} that are from JSON-style maps are equal.
     *
     * @param first the first list
     * @param second the second list
     */
    public static void assertListEquals(@Nullable List first, @Nullable List second) {
        assertSameNullity(first, second);
        if (first == null || second == null) {
            return;
        }

        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            final Object firstObject = first.get(i);
            final Object secondObject = second.get(i);
            assertMapValuesEqual(firstObject, secondObject);
        }
    }

    private static void assertMapValuesEqual(@Nullable Object firstObject,
                                             @Nullable Object secondObject) {
        if (firstObject instanceof Map) {
            assertTrue(secondObject instanceof Map);
            //noinspection unchecked
            assertMapEquals((Map<String, Object>) firstObject, (Map<String, Object>) secondObject);
        } else if (firstObject instanceof List) {
            assertTrue(secondObject instanceof List);
            assertListEquals((List) firstObject, (List) secondObject);
        } else {
            assertEquals(firstObject, secondObject);
        }
    }

    /**
     * Checks to see if both objects are {@code null} or non-null. Fails if one is {@code null}
     * while the other is not.
     *
     * @param first the first object to check
     * @param second the second object to check
     */
    private static void assertSameNullity(@Nullable Object first, @Nullable Object second) {
        boolean sameNullity = (first == null) == (second == null);
        assertTrue(sameNullity);
    }
}
