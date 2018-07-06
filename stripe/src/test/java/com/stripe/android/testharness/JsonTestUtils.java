package com.stripe.android.testharness;

import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Utility class for testing with JSON objects and maps that are created from or for JSON.
 */
public class JsonTestUtils {

    /**
     * Assert that two {@link JSONObject JSONObjects} are equal, comparing key by key recursively.
     *
     * @param first the first object
     * @param second the second object
     */
    public static void assertJsonEquals(JSONObject first, JSONObject second) {
        if (assertSameNullity(first, second)) {
            return;
        }

        assertEquals(first.length(), second.length());
        Iterator<String> keyIterator = first.keys();
        while(keyIterator.hasNext()) {
            String key = keyIterator.next();
            String errorMessage = getKeyErrorMessage(key);
            assertTrue(errorMessage, second.has(key));
            if (first.opt(key) instanceof JSONObject) {
                assertTrue(errorMessage, second.opt(key) instanceof JSONObject);
                assertJsonEquals(first.optJSONObject(key), second.optJSONObject(key));
            } else if (first.opt(key) instanceof JSONArray) {
                assertTrue(errorMessage, second.opt(key) instanceof JSONArray);
                assertJsonArrayEquals(first.optJSONArray(key), second.optJSONArray(key));
            } else if (first.opt(key) instanceof Number) {
                assertTrue(errorMessage, second.opt(key) instanceof Number);
                assertEquals(errorMessage,
                        ((Number) first.opt(key)).longValue(),
                        ((Number) second.opt(key)).longValue());
            } else {
                assertEquals(errorMessage, first.opt(key), second.opt(key));
            }
        }
    }

    /**
     * Assert that two {@link JSONObject JSONObjects} are equal, comparing key by key recursively.
     * Ignores nulls.
     *
     * @param first the first object
     * @param second the second object
     */
    public static void assertJsonEqualsExcludingNulls(JSONObject first, JSONObject second) {
        if (assertSameNullity(first, second)) {
            return;
        }

        Iterator<String> keyIterator = first.keys();
        while(keyIterator.hasNext()) {
            String key = keyIterator.next();
            String errorMessage = getKeyErrorMessage(key);
            if (first.opt(key) != JSONObject.NULL) {
                assertTrue(errorMessage, second.has(key));
            }
            if (first.opt(key) instanceof JSONObject) {
                assertTrue(errorMessage, second.opt(key) instanceof JSONObject);
                assertJsonEqualsExcludingNulls(first.optJSONObject(key), second.optJSONObject(key));
            } else if (first.opt(key) instanceof JSONArray) {
                assertTrue(errorMessage, second.opt(key) instanceof JSONArray);
                assertJsonArrayEquals(first.optJSONArray(key), second.optJSONArray(key));
            } else if (first.opt(key) instanceof Number) {
                assertTrue(errorMessage, second.opt(key) instanceof Number);
                assertEquals(errorMessage,
                        ((Number) first.opt(key)).longValue(),
                        ((Number) second.opt(key)).longValue());
            } else {
                assertEquals(errorMessage, first.opt(key), second.opt(key));
            }
        }
    }


    /**
     * Assert that two {@link JSONArray JSONArrays} are equal, comparing index by index recursively.
     *
     * @param first the first array
     * @param second the second array
     */
    public static void assertJsonArrayEquals(JSONArray first, JSONArray second) {
        if (assertSameNullity(first, second)) {
            return;
        }
        assertEquals(first.length(), second.length());
        for (int i = 0; i < first.length(); i++) {
            if (first.opt(i) instanceof JSONObject) {
                assertTrue(second.opt(i) instanceof JSONObject);
                assertJsonEquals(first.optJSONObject(i), second.optJSONObject(i));
            } else if (first.opt(i) instanceof JSONArray) {
                assertTrue(second.opt(i) instanceof JSONArray);
                assertJsonArrayEquals(first.optJSONArray(i), second.optJSONArray(i));
            } else if (first.opt(i) instanceof Number) {
                assertTrue(second.opt(i) instanceof Number);
                assertEquals(((Number) first.opt(i)).longValue(),
                             ((Number) second.opt(i)).longValue());
            } else {
                assertEquals(first.opt(i), second.opt(i));
            }
        }
    }

    /**
     * Assert that two JSON-style {@link Map Maps} are equal, comparing key by key recursively.
     *
     * @param first the first map
     * @param second the second map
     */
    public static void assertMapEquals(Map<String, ? extends Object> first,
                                       Map<String, ? extends Object> second) {
        if (assertSameNullity(first, second)) {
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
    public static void assertListEquals(List first, List second) {
        if (assertSameNullity(first, second)) {
            return;
        }

        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            Object firstObject = first.get(i);
            Object secondObject = second.get(i);
            assertMapValuesEqual(firstObject, secondObject);
        }
    }

    private static void assertMapValuesEqual(Object firstObject, Object secondObject) {
        if (firstObject instanceof Map) {
            assertTrue(secondObject instanceof Map);
            assertMapEquals(castMapWithGenerics((Map) firstObject),
                    castMapWithGenerics((Map) secondObject));
        } else if (firstObject instanceof List) {
            assertTrue(secondObject instanceof List);
            assertListEquals((List) firstObject,
                    (List) secondObject);
        } else {
            assertEquals(firstObject, secondObject);
        }
    }

    /**
     * A utility function whose primary purpose is to only suppress warnings one time.
     *
     * @param needsCasting a {@link Map} object that shoudl be String-keyed.
     * @return a cast map
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMapWithGenerics(Map needsCasting) {
        return (Map<String, Object>) needsCasting;
    }

    /**
     * Checks to see if both objects are {@code null} or non-null. Fails if one is {@code null}
     * while the other is not.
     *
     * @param first the first object to check
     * @param second the second object to check
     * @return {@code false} if both objects are non-null, {@code true} if they are {@code null}
     */
    private static boolean assertSameNullity(@Nullable Object first, @Nullable Object second) {
        boolean sameNullity = first == null
                ? second == null
                : second != null;
        assertTrue(sameNullity);
        return first == null;
    }

    private static String getKeyErrorMessage(String key) {
        return String.format(Locale.ENGLISH, "Matching error at key %s", key);
    }
}
