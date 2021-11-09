package com.stripe.android.core.exception

import com.stripe.android.core.StripeErrorFixtures
import java.net.HttpURLConnection
import kotlin.test.Test
import kotlin.test.assertEquals

class InvalidRequestExceptionTest {
    @Test
    fun stripeError_shouldReturnStripeError() {
        val stripeException = InvalidRequestException(
            StripeErrorFixtures.INVALID_REQUEST_ERROR,
            "req_123",
            HttpURLConnection.HTTP_BAD_REQUEST
        )
        assertEquals(StripeErrorFixtures.INVALID_REQUEST_ERROR, stripeException.stripeError)
    }

    @Test
    fun init_shouldDefaultMessageToStripeErrorMessage() {
        assertEquals(
            "This payment method (bancontact) is not activated for your account.",
            InvalidRequestException(StripeErrorFixtures.INVALID_REQUEST_ERROR).message
        )
    }

    @Test
    fun toString_withRequestId() {
        val actual = InvalidRequestException(
            requestId = "req_123",
            cause = IllegalArgumentException()
        ).toString()
        val expected =
            """
            Request-id: req_123
            com.stripe.android.core.exception.InvalidRequestException
            """.trimIndent()
        assertEquals(expected, actual)
    }

    @Test
    fun toString_withoutRequestId() {
        val actual = InvalidRequestException(
            cause = IllegalArgumentException()
        ).toString()
        val expected = "com.stripe.android.core.exception.InvalidRequestException"
        assertEquals(expected, actual)
    }
}
