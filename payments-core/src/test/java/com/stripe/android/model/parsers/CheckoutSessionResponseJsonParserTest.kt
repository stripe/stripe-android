package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntent
import org.json.JSONObject
import org.junit.Test

class CheckoutSessionResponseJsonParserTest {

    @Test
    fun `parse returns CheckoutSessionResponse with valid JSON`() {
        val parser = CheckoutSessionResponseJsonParser(isLiveMode = false)
        val result = parser.parse(VALID_CHECKOUT_SESSION_RESPONSE_JSON)

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo("cs_test_123")
        assertThat(result?.amount).isEqualTo(1000L)
        assertThat(result?.currency).isEqualTo("usd")
        assertThat(result?.elementsSession).isNotNull()
    }

    @Test
    fun `parse extracts elementsSession correctly`() {
        val parser = CheckoutSessionResponseJsonParser(isLiveMode = false)
        val result = parser.parse(VALID_CHECKOUT_SESSION_RESPONSE_JSON)

        assertThat(result).isNotNull()
        val elementsSession = result?.elementsSession

        assertThat(elementsSession?.stripeIntent).isNotNull()
        assertThat(elementsSession?.stripeIntent).isInstanceOf(PaymentIntent::class.java)
        assertThat(elementsSession?.merchantCountry).isEqualTo("US")
        assertThat(elementsSession?.isGooglePayEnabled).isTrue()
    }

    @Test
    fun `parse returns null when id is missing`() {
        val json = JSONObject(
            """
            {
                "amount": 1000,
                "currency": "usd",
                "elements_session": $MINIMAL_ELEMENTS_SESSION_JSON
            }
            """.trimIndent()
        )
        val parser = CheckoutSessionResponseJsonParser(isLiveMode = false)

        assertThat(parser.parse(json)).isNull()
    }

    @Test
    fun `parse returns null when amount is missing`() {
        val json = JSONObject(
            """
            {
                "id": "cs_test_123",
                "currency": "usd",
                "elements_session": $MINIMAL_ELEMENTS_SESSION_JSON
            }
            """.trimIndent()
        )
        val parser = CheckoutSessionResponseJsonParser(isLiveMode = false)

        assertThat(parser.parse(json)).isNull()
    }

    @Test
    fun `parse returns null when currency is missing`() {
        val json = JSONObject(
            """
            {
                "id": "cs_test_123",
                "amount": 1000,
                "elements_session": $MINIMAL_ELEMENTS_SESSION_JSON
            }
            """.trimIndent()
        )
        val parser = CheckoutSessionResponseJsonParser(isLiveMode = false)

        assertThat(parser.parse(json)).isNull()
    }

    @Test
    fun `parse returns null when elements_session is missing`() {
        val json = JSONObject(
            """
            {
                "id": "cs_test_123",
                "amount": 1000,
                "currency": "usd"
            }
            """.trimIndent()
        )
        val parser = CheckoutSessionResponseJsonParser(isLiveMode = false)

        assertThat(parser.parse(json)).isNull()
    }

    @Test
    fun `parse returns null when elements_session is invalid`() {
        val json = JSONObject(
            """
            {
                "id": "cs_test_123",
                "amount": 1000,
                "currency": "usd",
                "elements_session": {}
            }
            """.trimIndent()
        )
        val parser = CheckoutSessionResponseJsonParser(isLiveMode = false)

        assertThat(parser.parse(json)).isNull()
    }

    @Test
    fun `parse handles zero amount`() {
        val json = JSONObject(
            """
            {
                "id": "cs_test_123",
                "amount": 0,
                "currency": "usd",
                "elements_session": $MINIMAL_ELEMENTS_SESSION_JSON
            }
            """.trimIndent()
        )
        val parser = CheckoutSessionResponseJsonParser(isLiveMode = false)
        val result = parser.parse(json)

        assertThat(result).isNotNull()
        assertThat(result?.amount).isEqualTo(0L)
    }

    companion object {
        /**
         * Minimal elements_session JSON that satisfies ElementsSessionJsonParser.
         * Uses deferred_intent type which is what checkout sessions return.
         */
        private const val MINIMAL_ELEMENTS_SESSION_JSON = """
            {
                "session_id": "elements_session_test_123",
                "merchant_country": "US",
                "google_pay_preference": "enabled",
                "payment_method_preference": {
                    "object": "payment_method_preference",
                    "country_code": "US",
                    "ordered_payment_method_types": ["card", "link"],
                    "type": "deferred_intent"
                }
            }
        """

        private val VALID_CHECKOUT_SESSION_RESPONSE_JSON = JSONObject(
            """
            {
                "id": "cs_test_123",
                "amount": 1000,
                "currency": "usd",
                "elements_session": {
                    "session_id": "elements_session_test_123",
                    "merchant_country": "US",
                    "google_pay_preference": "enabled",
                    "link_settings": {
                        "link_bank_enabled": false,
                        "link_bank_onboarding_enabled": false
                    },
                    "payment_method_preference": {
                        "object": "payment_method_preference",
                        "country_code": "US",
                        "ordered_payment_method_types": ["card", "link", "us_bank_account"],
                        "type": "deferred_intent"
                    }
                }
            }
            """.trimIndent()
        )
    }
}
