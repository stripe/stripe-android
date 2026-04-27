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
                BOILERPLATE_SUFFIX,
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
                BOILERPLATE_SUFFIX,
            ex.message
        )
    }

    @Test
    fun testCreateWithUrlRedactsSensitiveQueryParams() {
        val ex = APIConnectionException.create(
            IOException("Unable to resolve host"),
            "https://api.stripe.com/v1/elements/sessions?" +
                "legacy_customer_ephemeral_key=ek_live_secret123&" +
                "type=deferred_intent&locale=en-US&" +
                "mobile_session_id=fc2e200b-581a-4f24-93b6-990912a337db"
        )
        assertEquals(
            "IOException during API request to Stripe " +
                "(https://api.stripe.com/v1/elements/sessions?" +
                "legacy_customer_ephemeral_key=**REDACTED**&" +
                "type=deferred_intent&locale=en-US&" +
                "mobile_session_id=fc2e200b-581a-4f24-93b6-990912a337db): " +
                "Unable to resolve host. " +
                BOILERPLATE_SUFFIX,
            ex.message
        )
    }

    @Test
    fun testCreateRedactsValuesThatLookLikeStripeKeys() {
        val ex = APIConnectionException.create(
            IOException("fail"),
            "https://api.stripe.com/v1/foo?key=pk_live_abc123&other=safe_value"
        )
        assertEquals(
            "IOException during API request to Stripe " +
                "(https://api.stripe.com/v1/foo?" +
                "key=**REDACTED**&other=safe_value): fail. " +
                BOILERPLATE_SUFFIX,
            ex.message
        )
    }

    @Test
    fun testCreateRedactsClientSecret() {
        val ex = APIConnectionException.create(
            IOException("fail"),
            "https://api.stripe.com/v1/foo?client_secret=pi_secret_value&mode=payment"
        )
        assertEquals(
            "IOException during API request to Stripe " +
                "(https://api.stripe.com/v1/foo?" +
                "client_secret=**REDACTED**&mode=payment): fail. " +
                BOILERPLATE_SUFFIX,
            ex.message
        )
    }

    @Test
    fun testCreateRedactsUrlEncodedParamNames() {
        val ex = APIConnectionException.create(
            IOException("fail"),
            "https://api.stripe.com/v1/foo?client%5Fsecret=some_value&mode=payment"
        )
        assertEquals(
            "IOException during API request to Stripe " +
                "(https://api.stripe.com/v1/foo?" +
                "client_secret=**REDACTED**&mode=payment): fail. " +
                BOILERPLATE_SUFFIX,
            ex.message
        )
    }

    private companion object {
        private const val BOILERPLATE_SUFFIX =
            "Please check your internet connection and try again. " +
                "If this problem persists, you should check Stripe's service " +
                "status at https://status.stripe.com/, " +
                "or let us know at support@stripe.com."
    }
}
