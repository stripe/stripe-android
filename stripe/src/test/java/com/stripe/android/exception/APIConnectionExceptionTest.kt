package com.stripe.android.exception

import kotlin.test.Test
import kotlin.test.assertEquals

class APIConnectionExceptionTest {

    @Test
    fun testCreate() {
        val ex = APIConnectionException.create(
            "https://api.stripe.com/v1/payment_methods",
            IllegalArgumentException("Invalid id")
        )
        assertEquals(
            "IOException during API request to Stripe " +
            "(https://api.stripe.com/v1/payment_methods): Invalid id. " +
            "Please check your internet connection and try again. " +
            "If this problem persists, you should check Stripe's service " +
            "status at https://twitter.com/stripestatus, " +
            "or let us know at support@stripe.com.",
            ex.message
        )
    }
}
