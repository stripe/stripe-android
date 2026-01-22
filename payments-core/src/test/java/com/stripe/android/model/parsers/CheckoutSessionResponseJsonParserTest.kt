package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CheckoutSessionFixtures
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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

    // Confirm response tests

    @Test
    fun `parse confirm response with succeeded payment intent`() {
        val params = ElementsSessionParams.CheckoutSessionType(clientSecret = "cs_test_secret")
        val result = CheckoutSessionResponseJsonParser(params, isLiveMode = false)
            .parse(CheckoutSessionFixtures.CHECKOUT_SESSION_CONFIRM_SUCCEEDED_JSON)

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo("ppage_1SrjAuLu5o3P18ZpavYVO6Xq")
        assertThat(result?.amount).isEqualTo(999L)
        assertThat(result?.currency).isEqualTo("usd")

        // Verify PaymentIntent is parsed
        val paymentIntent = result?.paymentIntent
        assertThat(paymentIntent).isNotNull()
        assertThat(paymentIntent?.id).isEqualTo("pi_3QWK2VIyGgrkZxL71xfPBWG5")
        assertThat(paymentIntent?.status).isEqualTo(StripeIntent.Status.Succeeded)
        assertThat(paymentIntent?.isConfirmed).isTrue()

        // Confirm responses don't include elements_session
        assertThat(result?.elementsSession).isNull()
    }

    @Test
    fun `parse confirm response with requires_action payment intent`() {
        val params = ElementsSessionParams.CheckoutSessionType(clientSecret = "cs_test_secret")
        val result = CheckoutSessionResponseJsonParser(params, isLiveMode = false)
            .parse(CheckoutSessionFixtures.CHECKOUT_SESSION_CONFIRM_REQUIRES_ACTION_JSON)

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo("ppage_1SrjAuLu5o3P18ZpavYVO6Xq")

        // Verify PaymentIntent has requires_action status
        val paymentIntent = result?.paymentIntent
        assertThat(paymentIntent).isNotNull()
        assertThat(paymentIntent?.id).isEqualTo("pi_3QWK2VIyGgrkZxL71xfPBWG5")
        assertThat(paymentIntent?.status).isEqualTo(StripeIntent.Status.RequiresAction)
        assertThat(paymentIntent?.requiresAction()).isTrue()

        // Verify next_action is parsed
        assertThat(paymentIntent?.nextActionType).isEqualTo(StripeIntent.NextActionType.RedirectToUrl)
    }

    @Test
    fun `parse confirm response uses payment intent id when checkout session id is missing`() {
        val json = JSONObject(
            """
            {
                "payment_intent": {
                    "id": "pi_123",
                    "object": "payment_intent",
                    "amount": 500,
                    "currency": "eur",
                    "status": "succeeded",
                    "client_secret": "pi_123_secret_abc",
                    "payment_method_types": ["card"],
                    "livemode": false,
                    "created": 1734000000
                }
            }
            """.trimIndent()
        )
        val params = ElementsSessionParams.CheckoutSessionType(clientSecret = "cs_test_secret")
        val result = CheckoutSessionResponseJsonParser(params, isLiveMode = false).parse(json)

        // Should use payment intent ID as fallback
        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo("pi_123")
        assertThat(result?.amount).isEqualTo(500L)
        assertThat(result?.currency).isEqualTo("eur")
    }

    @Test
    fun `parse confirm response returns null when payment intent has no id`() {
        val json = JSONObject(
            """
            {
                "payment_intent": {
                    "object": "payment_intent",
                    "amount": 500,
                    "currency": "eur",
                    "status": "succeeded"
                }
            }
            """.trimIndent()
        )
        val params = ElementsSessionParams.CheckoutSessionType(clientSecret = "cs_test_secret")
        val result = CheckoutSessionResponseJsonParser(params, isLiveMode = false).parse(json)

        // Should return null when no ID can be determined
        assertThat(result).isNull()
    }
}
