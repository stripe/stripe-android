package com.stripe.android

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class StripeResponseTest {

    @Test
    fun requestId_handlesLowerCaseKey() {
        val response = StripeResponse(
            responseCode = 200,
            responseBody = "{}",
            responseHeaders = mapOf("request-id" to listOf("req_12345"))
        )

        assertThat(response.requestId)
            .isEqualTo("req_12345")
    }

    @Test
    fun requestId_handlesTitleCaseKey() {
        val response = StripeResponse(
            responseCode = 200,
            responseBody = "{}",
            responseHeaders = mapOf("Request-Id" to listOf("req_12345"))
        )

        assertThat(response.requestId)
            .isEqualTo("req_12345")
    }
}
