package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.exception.APIException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class StripeResponseTest {

    @Test
    fun requestId_handlesLowerCaseKey() {
        val response = StripeResponse(
            code = 200,
            body = "{}",
            headers = mapOf("request-id" to listOf("req_12345"))
        )

        assertThat(response.requestId)
            .isEqualTo("req_12345")
    }

    @Test
    fun requestId_handlesTitleCaseKey() {
        val response = StripeResponse(
            code = 200,
            body = "{}",
            headers = mapOf("Request-Id" to listOf("req_12345"))
        )

        assertThat(response.requestId)
            .isEqualTo("req_12345")
    }

    @Test
    fun responseJson_withNonJsonBody_shouldThrowApiException() {
        val exception = assertFailsWith<APIException> {
            StripeResponse(
                code = 500,
                body = "there was an error",
                headers = mapOf(
                    "Request-Id" to listOf("req_12345"),
                    "Content-Type" to listOf("text/plain")
                )
            ).responseJson
        }
        val expectedMessage = """
                Exception while parsing response body.
                  Status code: 500
                  Request-Id: req_12345
                  Content-Type: text/plain
                  Body: "there was an error"
        """.trimIndent()
        assertThat(exception.message)
            .isEqualTo(expectedMessage)
    }
}
