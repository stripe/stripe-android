package com.stripe.android.testharness

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * Utility class for testing with JSON objects and maps that are created from or for JSON.
 */
object JsonTestUtils {
    /**
     * Assert that two JSON-style [Maps][Map] are equal, comparing key by key recursively.
     *
     * @param first the first map
     * @param second the second map
     */
    fun assertMapEquals(first: Map<String, *>?, second: Map<String, *>?) {
        assertSameNullity(first, second)
        if (first == null || second == null) {
            return
        }

        assertEquals(first.size.toLong(), second.size.toLong())
        for (key in first.keys) {
            assertTrue(second.containsKey(key))
            val firstObject = first[key]
            val secondObject = second[key]
            assertMapValuesEqual(firstObject, secondObject)
        }
    }

    /**
     * Assert two [Lists][List] that are from JSON-style maps are equal.
     *
     * @param first the first list
     * @param second the second list
     */
    fun assertListEquals(first: List<*>?, second: List<*>?) {
        assertSameNullity(first, second)
        if (first == null || second == null) {
            return
        }

        assertEquals(first.size.toLong(), second.size.toLong())
        for (i in first.indices) {
            val firstObject = first[i]
            val secondObject = second[i]
            assertMapValuesEqual(firstObject, secondObject)
        }
    }

    private fun assertMapValuesEqual(
        firstObject: Any?,
        secondObject: Any?
    ) {
        if (firstObject is Map<*, *>) {
            assertTrue(secondObject is Map<*, *>)

            assertMapEquals(firstObject as Map<String, Any>?, secondObject as Map<String, Any>?)
        } else if (firstObject is List<*>) {
            assertTrue(secondObject is List<*>)
            assertListEquals(firstObject as List<*>?, secondObject as List<*>?)
        } else {
            assertEquals(firstObject, secondObject)
        }
    }

    /**
     * Checks to see if both objects are `null` or non-null. Fails if one is `null`
     * while the other is not.
     *
     * @param first the first object to check
     * @param second the second object to check
     */
    private fun assertSameNullity(first: Any?, second: Any?) {
        val sameNullity = first == null == (second == null)
        assertTrue(sameNullity)
    }
}
