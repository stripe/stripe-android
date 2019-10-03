package com.stripe.android.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentIntentTest {

    @Test
    fun getAuthorizationUrl_whenProvidedBadUrl_doesNotCrash() {
        val paymentIntent = requireNotNull(PaymentIntent.fromJson(
            PAYMENT_INTENT_WITH_SOURCE_WITH_BAD_AUTH_URL_JSON
        ))

        val authUrl = paymentIntent.redirectUrl
        assertNotNull(authUrl)
        assertEquals(BAD_URL, authUrl.encodedPath)
    }

    @Test
    fun getRedirectUrl_withRedirectToUrlPopulate_returnsRedirectUrl() {
        val paymentIntent = requireNotNull(
            PaymentIntent.fromJson(PARTIAL_PAYMENT_INTENT_WITH_REDIRECT_URL_JSON)
        )
        assertTrue(paymentIntent.requiresAction())
        assertEquals(StripeIntent.NextActionType.RedirectToUrl, paymentIntent.nextActionType)
        val redirectUrl = paymentIntent.redirectUrl
        assertNotNull(redirectUrl)
        assertEquals("https://example.com/redirect", redirectUrl.toString())
    }

    @Test
    fun getRedirectUrl_withAuthorizeWithUrlPopulated_returnsRedirectUrl() {
        val paymentIntent = PaymentIntent
            .fromJson(PARTIAL_PAYMENT_INTENT_WITH_AUTHORIZE_WITH_URL_JSON)
        assertNotNull(paymentIntent)
        assertEquals(StripeIntent.NextActionType.RedirectToUrl, paymentIntent.nextActionType)
        val redirectUrl = paymentIntent.redirectUrl
        assertNotNull(redirectUrl)
        assertEquals("https://example.com/redirect", redirectUrl.toString())
    }

    @Test
    fun parseIdFromClientSecret_parsesCorrectly() {
        val clientSecret = "pi_1CkiBMLENEVhOs7YMtUehLau_secret_s4O8SDh7s6spSmHDw1VaYPGZA"
        val id = PaymentIntent.parseIdFromClientSecret(clientSecret)
        assertEquals("pi_1CkiBMLENEVhOs7YMtUehLau", id)
    }

    @Test
    fun parsePaymentIntentWithPaymentMethods() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2
        assertTrue(paymentIntent.requiresAction())
        assertEquals("card", paymentIntent.paymentMethodTypes[0])
        assertEquals(0, paymentIntent.canceledAt)
        assertEquals("automatic", paymentIntent.captureMethod)
        assertEquals("manual", paymentIntent.confirmationMethod)
        assertNotNull(paymentIntent.nextAction)
        assertEquals("jenny@example.com", paymentIntent.receiptEmail)
        assertNull(paymentIntent.cancellationReason)
    }

    @Test
    fun getNextActionTypeAndStripeSdkData_whenUseStripeSdkWith3ds2() {
        assertEquals(StripeIntent.NextActionType.UseStripeSdk,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.nextActionType)
        val sdkData = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.stripeSdkData
        assertNotNull(sdkData)
        assertTrue(sdkData.is3ds2)
        assertEquals("mastercard", sdkData.data["directory_server_name"])
    }

    @Test
    fun getNextActionTypeAndStripeSdkData_whenUseStripeSdkWith3ds1() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_3DS1
        assertEquals(StripeIntent.NextActionType.UseStripeSdk, paymentIntent.nextActionType)
        val sdkData = requireNotNull(paymentIntent.stripeSdkData)
        assertTrue(sdkData.is3ds1)
        assertNotNull(sdkData.data["stripe_js"])
    }

    @Test
    fun getNextActionTypeAndStripeSdkData_whenRedirectToUrl() {
        assertEquals(StripeIntent.NextActionType.RedirectToUrl,
            PaymentIntentFixtures.PI_REQUIRES_REDIRECT.nextActionType)
        assertNull(PaymentIntentFixtures.PI_REQUIRES_REDIRECT.stripeSdkData)
    }

    @Test
    fun getLastPaymentError_parsesCorrectly() {
        val lastPaymentError =
            requireNotNull(PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR.lastPaymentError)
        assertEquals("pm_1F7J1bCRMbs6FrXfQKsYwO3U", lastPaymentError.paymentMethod?.id)
        assertEquals("payment_intent_authentication_failure", lastPaymentError.code)
        assertEquals(PaymentIntent.Error.Type.InvalidRequestError, lastPaymentError.type)
        assertEquals(
            "https://stripe.com/docs/error-codes/payment-intent-authentication-failure",
            lastPaymentError.docUrl
        )
        assertEquals(
            "The provided PaymentMethod has failed authentication. You can provide payment_method_data or a new PaymentMethod to attempt to fulfill this PaymentIntent again.",
            lastPaymentError.message
        )
    }

    @Test
    fun testCanceled() {
        assertEquals(StripeIntent.Status.Canceled,
            PaymentIntentFixtures.CANCELLED.status)
        assertEquals(PaymentIntent.CancellationReason.Abandoned,
            PaymentIntentFixtures.CANCELLED.cancellationReason)
        assertEquals(1567091866L,
            PaymentIntentFixtures.CANCELLED.canceledAt)
    }

    companion object {
        private const val BAD_URL: String = "nonsense-blahblah"

        private val PAYMENT_INTENT_WITH_SOURCE_WITH_BAD_AUTH_URL_JSON = JSONObject(
            """
            {
                "id": "pi_1CkiBMLENEVhOs7YMtUehLau",
                "object": "payment_intent",
                "amount": 1000,
                "canceled_at": 1530839340,
                "capture_method": "automatic",
                "client_secret": "pi_1CkiBMLENEVhOs7YMtUehLau_secret_s4O8SDh7s6spSmHDw1VaYPGZA",
                "confirmation_method": "publishable",
                "created": 1530838340,
                "currency": "usd",
                "description": "Example PaymentIntent charge",
                "livemode": false,
                "next_action": {
                    "type": "redirect_to_url",
                    "redirect_to_url": {
                        "url": "nonsense-blahblah",
                        "return_url": "yourapp://post-authentication-return-url"
                    }
                },
                "receipt_email": null,
                "shipping": null,
                "source": "src_1CkiC3LENEVhOs7YMSa4yx4G",
                "status": "requires_action"
            }
            """.trimIndent()
        )

        private val PARTIAL_PAYMENT_INTENT_WITH_REDIRECT_URL_JSON = JSONObject(
            """
            {
                "id": "pi_Aabcxyz01aDfoo",
                "object": "payment_intent",
                "status": "requires_action",
                "next_action": {
                    "type": "redirect_to_url",
                    "redirect_to_url": {
                        "url": "https://example.com/redirect",
                        "return_url": "yourapp://post-authentication-return-url"
                    }
                }
            }
            """.trimIndent()
        )

        private val PARTIAL_PAYMENT_INTENT_WITH_AUTHORIZE_WITH_URL_JSON = JSONObject(
            """
            {
                "id": "pi_Aabcxyz01aDfoo",
                "object": "payment_intent",
                "status": "requires_action",
                "next_action": {
                    "type": "redirect_to_url",
                    "redirect_to_url": {
                        "url": "https://example.com/redirect",
                        "return_url": "yourapp://post-authentication-return-url"
                    }
                }
            }
            """.trimIndent()
        )
    }
}
