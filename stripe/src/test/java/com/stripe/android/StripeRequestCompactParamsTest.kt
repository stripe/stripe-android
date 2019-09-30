package com.stripe.android

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class StripeRequestCompactParamsTest {
    @Test
    fun compatParams_removesNullParams() {
        val params = mapOf("a" to null, "b" to "not null")

        val compactParams = getCompactedParams(params)
        assertEquals(1, compactParams.size)
        assertTrue(compactParams.containsKey("b"))
    }

    @Test
    fun compatParams_removesEmptyStringParams() {
        val params = mapOf(
            "a" to "fun param",
            "b" to "not null",
            "c" to ""
        )
        val compactParams = getCompactedParams(params)
        assertEquals(2, compactParams.size)
        assertTrue(compactParams.containsKey("a"))
        assertTrue(compactParams.containsKey("b"))
    }

    @Test
    fun compatParams_removesNestedEmptyParams() {
        val outParams = getCompactedParams(createParamsWithNestedMap())

        assertEquals(3, outParams.size)
        assertTrue(outParams.containsKey("a"))
        assertTrue(outParams.containsKey("b"))
        assertTrue(outParams.containsKey("c"))

        val firstNestedMap = outParams["c"] as Map<String, Any>
        assertEquals(2, firstNestedMap.size)
        assertTrue(firstNestedMap.containsKey("1a"))
        assertTrue(firstNestedMap.containsKey("1c"))

        val secondNestedMap = firstNestedMap["1c"] as Map<String, Any>
        assertEquals(1, secondNestedMap.size)
        assertTrue(secondNestedMap.containsKey("2b"))
    }

    private fun createParamsWithNestedMap(): Map<String, Any> {
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
            )
        )
    }

    private fun getCompactedParams(params: Map<String, Any?>): Map<String, *> {
        return FakeRequest(params).params!!
    }

    private class FakeRequest internal constructor(
        params: Map<String, *>?
    ) : StripeRequest(
        Method.POST,
        "https://example.com",
        params,
        "application/x-www-form-urlencoded"
    ) {
        override fun getUserAgent(): String {
            return ""
        }

        override fun getOutputBytes(): ByteArray {
            return ByteArray(0)
        }

        override fun createHeaders(): Map<String, String> {
            return emptyMap()
        }
    }
}
