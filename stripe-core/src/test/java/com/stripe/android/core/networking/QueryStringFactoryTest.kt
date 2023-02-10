package com.stripe.android.core.networking

import java.net.URLDecoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QueryStringFactoryTest {
    @Test
    fun create_withSimpleParams() {
        val queryString = QueryStringFactory.create(
            mapOf(
                "color" to "blue"
            )
        )
        assertEquals(
            "color=blue",
            queryString
        )
    }

    @Test
    fun create_withComplexParams() {
        val queryString = QueryStringFactory.create(
            mapOf(
                "colors" to listOf("blue", "green"),
                "empty_list" to emptyList<String>(),
                "empty_map" to mapOf<String, String>(),
                "person" to mapOf(
                    "age" to 45,
                    "city" to "San Francisco",
                    "wishes" to emptyList<String>(),
                    "friends" to listOf("Alice", "Bob")
                )
            )
        )
        assertEquals(
            "colors[]=blue&colors[]=green&empty_list=&person[age]=45&person[city]=San Francisco&person[wishes]=&person[friends][]=Alice&person[friends][]=Bob",
            URLDecoder.decode(queryString, Charsets.UTF_8.name())
        )
    }

    @Test
    fun compatParams_removesNullParams() {
        val params = mapOf("a" to null, "b" to "not null")

        val compactParams = getCompactedParams(params)
        assertEquals(1, compactParams.size)
        assertTrue(compactParams.containsKey("b"))
    }

    @Test
    fun compatParams_doesNotRemoveEmptyStringParams() {
        // This was important for setting the future usage on a PI

        val params = mapOf(
            "a" to "fun param",
            "b" to "not null",
            "c" to ""
        )
        val compactParams = getCompactedParams(params)
        assertEquals(3, compactParams.size)
        assertTrue(compactParams.containsKey("a"))
        assertTrue(compactParams.containsKey("b"))
        assertTrue(compactParams.containsKey("c"))
    }

    @Test
    fun compatParams_removesNestedEmptyParams() {
        val outParams = getCompactedParams(createParamsWithNestedMap())

        assertEquals(3, outParams.size)
        assertTrue(outParams.containsKey("a"))
        assertTrue(outParams.containsKey("b"))
        assertTrue(outParams.containsKey("c"))
        assertFalse(outParams.containsKey("d"))

        val firstNestedMap = outParams["c"] as Map<*, *>
        assertEquals(2, firstNestedMap.size)
        assertTrue(firstNestedMap.containsKey("1a"))
        assertTrue(firstNestedMap.containsKey("1c"))

        val secondNestedMap = firstNestedMap["1c"] as Map<*, *>
        assertEquals(2, secondNestedMap.size)
        assertTrue(secondNestedMap.containsKey("2a"))
        assertTrue(secondNestedMap.containsKey("2b"))
    }

    private fun createParamsWithNestedMap(): Map<String, Any?> {
        return mapOf(
            "a" to "fun param",
            "b" to "not null",
            "c" to mapOf(
                "1a" to "something",
                "1b" to null,
                "1c" to mapOf(
                    "2a" to "",
                    "2b" to "hello world"
                )
            ),
            "d" to null
        )
    }

    private fun getCompactedParams(params: Map<String, Any?>): Map<String, *> {
        return QueryStringFactory.compactParams(params)
    }
}
