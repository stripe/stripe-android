package com.stripe.android.core.networking

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.StripeResponse.Companion.HEADER_REQUEST_ID
import kotlin.test.Test
import kotlin.test.assertFailsWith

class StripeResponseTest {

    @Test
    fun requestId_handlesLowerCaseKey() {
        val response = StripeResponse(
            code = 200,
            body = "{}",
            headers = mapOf(HEADER_REQUEST_ID to listOf("req_12345"))
        )

        assertThat(response.requestId)
            .isEqualTo(
                RequestId("req_12345")
            )
    }

    @Test
    fun requestId_handlesTitleCaseKey() {
        val response = StripeResponse(
            code = 200,
            body = "{}",
            headers = mapOf(HEADER_REQUEST_ID to listOf("req_12345"))
        )

        assertThat(response.requestId)
            .isEqualTo(
                RequestId("req_12345")
            )
    }

    @Test
    fun responseJson_withNonJsonBody_shouldThrowApiException() {
        val exception = assertFailsWith<APIException> {
            StripeResponse<String>(
                code = 500,
                body = "there was an error",
                headers = mapOf(
                    HEADER_REQUEST_ID to listOf("req_12345"),
                    HEADER_CONTENT_TYPE to listOf("text/plain")
                )
            ).responseJson()
        }
        val expectedMessage =
            """
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
