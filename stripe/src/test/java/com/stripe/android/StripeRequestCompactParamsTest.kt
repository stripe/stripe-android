package com.stripe.android

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        return FakeRequest(params).compactParams.orEmpty()
    }

    private class FakeRequest internal constructor(
        override val params: Map<String, *>?
    ) : StripeRequest() {
        override val method: Method = Method.POST
        override val baseUrl: String = "https://example.com"
        override val mimeType: MimeType = MimeType.Form
        override val body: String = ""
        override val headersFactory = RequestHeadersFactory.Default()
    }
}
