package com.stripe.android.core.exception

import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

class APIConnectionExceptionTest {

    @Test
    fun testCreateWithUrl() {
        val ex = APIConnectionException.create(
            IOException("Could not connect"),
            "https://api.stripe.com/v1/payment_methods"
        )
        assertEquals(
            "IOException during API request to Stripe " +
                "(https://api.stripe.com/v1/payment_methods): Could not connect. " +
                "Please check your internet connection and try again. " +
                "If this problem persists, you should check Stripe's service " +
                "status at https://twitter.com/stripestatus, " +
                "or let us know at support@stripe.com.",
            ex.message
        )
    }

    @Test
    fun testCreateWithoutUrl() {
        val ex = APIConnectionException.create(
            IOException("Could not connect")
        )
        assertEquals(
            "IOException during API request to Stripe: Could not connect. " +
                "Please check your internet connection and try again. " +
                "If this problem persists, you should check Stripe's service " +
                "status at https://twitter.com/stripestatus, " +
                "or let us know at support@stripe.com.",
            ex.message
        )
    }
}
