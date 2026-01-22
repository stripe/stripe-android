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
        assertThat(result?.id).isEqualTo("cs_test_a1vLTpmgcJO40ZjQpd3GUNHwlwtkT1bejjhpfd0nN05iqoVuJziixjNYIh")
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
    fun `parse returns null when session_id is missing`() {
        val json = JSONObject(
            """
            {
                "currency": "usd",
                "total_summary": { "due": 1000 },
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
                "session_id": "cs_test_123",
                "total_summary": { "due": 1000 },
                "elements_session": ${CheckoutSessionFixtures.MINIMAL_ELEMENTS_SESSION_JSON}
            }
            """.trimIndent()
        )
        val params = ElementsSessionParams.CheckoutSessionType(clientSecret = "cs_test_secret")
        val result = CheckoutSessionResponseJsonParser(params, isLiveMode = false).parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns null when total_summary is missing`() {
        val json = JSONObject(
            """
            {
                "session_id": "cs_test_123",
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
    fun `parse succeeds when elements_session is missing`() {
        val json = JSONObject(
            """
            {
                "session_id": "cs_test_123",
                "currency": "usd",
                "total_summary": { "due": 1000 }
            }
            """.trimIndent()
        )
        val params = ElementsSessionParams.CheckoutSessionType(clientSecret = "cs_test_secret")
        val result = CheckoutSessionResponseJsonParser(params, isLiveMode = false).parse(json)

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo("cs_test_123")
        assertThat(result?.amount).isEqualTo(1000L)
        assertThat(result?.currency).isEqualTo("usd")
        assertThat(result?.elementsSession).isNull()
    }

    @Test
    fun `parse returns null elements_session when it is invalid`() {
        val json = JSONObject(
            """
            {
                "session_id": "cs_test_123",
                "currency": "usd",
                "total_summary": { "due": 1000 },
                "elements_session": {}
            }
            """.trimIndent()
        )
        val params = ElementsSessionParams.CheckoutSessionType(clientSecret = "cs_test_secret")
        val result = CheckoutSessionResponseJsonParser(params, isLiveMode = false).parse(json)

        // Response is parsed successfully but elements_session is null due to invalid JSON
        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo("cs_test_123")
        assertThat(result?.elementsSession).isNull()
    }

    @Test
    fun `parse confirm response with succeeded payment intent`() {
        val params = ElementsSessionParams.CheckoutSessionType(clientSecret = "cs_test_secret")
        val result = CheckoutSessionResponseJsonParser(params, isLiveMode = false)
            .parse(CheckoutSessionFixtures.CHECKOUT_SESSION_CONFIRM_SUCCEEDED_JSON)

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo("cs_test_a1vLTpmgcJO40ZjQpd3GUNHwlwtkT1bejjhpfd0nN05iqoVuJziixjNYIh")
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
        assertThat(result?.id).isEqualTo("cs_test_a1vLTpmgcJO40ZjQpd3GUNHwlwtkT1bejjhpfd0nN05iqoVuJziixjNYIh")

        // Verify PaymentIntent has requires_action status
        val paymentIntent = result?.paymentIntent
        assertThat(paymentIntent).isNotNull()
        assertThat(paymentIntent?.id).isEqualTo("pi_3QWK2VIyGgrkZxL71xfPBWG5")
        assertThat(paymentIntent?.status).isEqualTo(StripeIntent.Status.RequiresAction)
        assertThat(paymentIntent?.requiresAction()).isTrue()

        // Verify next_action is parsed
        assertThat(paymentIntent?.nextActionType).isEqualTo(StripeIntent.NextActionType.RedirectToUrl)
    }
}
