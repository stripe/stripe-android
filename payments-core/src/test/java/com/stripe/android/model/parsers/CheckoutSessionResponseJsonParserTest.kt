package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CheckoutSessionFixtures
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.PaymentIntent
import org.json.JSONObject
import org.junit.Test

class CheckoutSessionResponseJsonParserTest {

    @Test
    fun `parse checkout session response`() {
        val params = ElementsSessionParams.CheckoutSessionType(
            clientSecret = "cs_test_123_secret_abc",
        )
        val result = CheckoutSessionResponseJsonParser(
            elementsSessionParams = params,
            isLiveMode = false,
        ).parse(CheckoutSessionFixtures.CHECKOUT_SESSION_RESPONSE_JSON)

        // Verify CheckoutSessionResponse fields
        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo("ppage_1SrjAuLu5o3P18ZpavYVO6Xq")
        assertThat(result?.amount).isEqualTo(999L)
        assertThat(result?.currency).isEqualTo("usd")

        // Verify ElementsSession is parsed correctly
        val elementsSession = result?.elementsSession
        assertThat(elementsSession).isNotNull()
        assertThat(elementsSession?.elementsSessionId).isEqualTo("elements_session_1nWWJQ3A6yS")
        assertThat(elementsSession?.merchantCountry).isEqualTo("US")
        assertThat(elementsSession?.isGooglePayEnabled).isTrue()

        // Verify StripeIntent is created correctly
        val stripeIntent = elementsSession?.stripeIntent
        assertThat(stripeIntent).isNotNull()
        assertThat(stripeIntent).isInstanceOf(PaymentIntent::class.java)

        // Verify payment method types from ordered_payment_method_types
        assertThat(stripeIntent?.paymentMethodTypes).containsExactly(
            "card",
            "link",
            "cashapp",
            "alipay",
            "wechat_pay",
            "us_bank_account",
            "amazon_pay",
            "afterpay_clearpay",
            "klarna",
            "crypto"
        ).inOrder()
    }

    @Test
    fun `parse returns null when id is missing`() {
        val json = JSONObject(
            """
            {
                "currency": "usd",
                "line_item_group": { "total": 1000 },
                "elements_session": ${CheckoutSessionFixtures.MINIMAL_ELEMENTS_SESSION_JSON}
            }
            """.trimIndent()
        )
        val params = ElementsSessionParams.CheckoutSessionType(clientSecret = "cs_test_secret")
        val result = CheckoutSessionResponseJsonParser(params, isLiveMode = false).parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns null when currency is missing`() {
        val json = JSONObject(
            """
            {
                "id": "cs_test_123",
                "line_item_group": { "total": 1000 },
                "elements_session": ${CheckoutSessionFixtures.MINIMAL_ELEMENTS_SESSION_JSON}
            }
            """.trimIndent()
        )
        val params = ElementsSessionParams.CheckoutSessionType(clientSecret = "cs_test_secret")
        val result = CheckoutSessionResponseJsonParser(params, isLiveMode = false).parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns null when line_item_group is missing`() {
        val json = JSONObject(
            """
            {
                "id": "cs_test_123",
                "currency": "usd",
                "elements_session": ${CheckoutSessionFixtures.MINIMAL_ELEMENTS_SESSION_JSON}
            }
            """.trimIndent()
        )
        val params = ElementsSessionParams.CheckoutSessionType(clientSecret = "cs_test_secret")
        val result = CheckoutSessionResponseJsonParser(params, isLiveMode = false).parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns null when elements_session is missing`() {
        val json = JSONObject(
            """
            {
                "id": "cs_test_123",
                "currency": "usd",
                "line_item_group": { "total": 1000 }
            }
            """.trimIndent()
        )
        val params = ElementsSessionParams.CheckoutSessionType(clientSecret = "cs_test_secret")
        val result = CheckoutSessionResponseJsonParser(params, isLiveMode = false).parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns null when elements_session is invalid`() {
        val json = JSONObject(
            """
            {
                "id": "cs_test_123",
                "currency": "usd",
                "line_item_group": { "total": 1000 },
                "elements_session": {}
            }
            """.trimIndent()
        )
        val params = ElementsSessionParams.CheckoutSessionType(clientSecret = "cs_test_secret")
        val result = CheckoutSessionResponseJsonParser(params, isLiveMode = false).parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse handles zero amount`() {
        val json = JSONObject(
            """
            {
                "id": "cs_test_123",
                "currency": "usd",
                "line_item_group": { "total": 0 },
                "elements_session": ${CheckoutSessionFixtures.MINIMAL_ELEMENTS_SESSION_JSON}
            }
            """.trimIndent()
        )
        val params = ElementsSessionParams.CheckoutSessionType(clientSecret = "cs_test_secret")
        val result = CheckoutSessionResponseJsonParser(params, isLiveMode = false).parse(json)

        assertThat(result).isNotNull()
        assertThat(result?.amount).isEqualTo(0L)
    }
}
