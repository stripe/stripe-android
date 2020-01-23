package com.stripe.android.exception

import com.stripe.android.StripeErrorFixtures
import kotlin.test.Test
import kotlin.test.assertEquals

class StripeExceptionTest {

    @Test
    fun testEquals() {
        assertEquals(
            InvalidRequestException(
                stripeError = StripeErrorFixtures.INVALID_REQUEST_ERROR,
                requestId = "req_123",
                e = RuntimeException()
            ),
            InvalidRequestException(
                stripeError = StripeErrorFixtures.INVALID_REQUEST_ERROR,
                requestId = "req_123",
                e = RuntimeException()
            )
        )
    }
}
