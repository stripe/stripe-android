package com.stripe.android.core.exception

import com.stripe.android.core.StripeErrorFixtures
import kotlin.test.Test
import kotlin.test.assertEquals

class StripeExceptionTest {

    @Test
    fun testEquals() {
        assertEquals(
            InvalidRequestException(
                stripeError = StripeErrorFixtures.INVALID_REQUEST_ERROR,
                requestId = "req_123",
                cause = RuntimeException()
            ),
            InvalidRequestException(
                stripeError = StripeErrorFixtures.INVALID_REQUEST_ERROR,
                requestId = "req_123",
                cause = RuntimeException()
            )
        )
    }
}
