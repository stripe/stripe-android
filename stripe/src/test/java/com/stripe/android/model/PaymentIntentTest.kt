package com.stripe.android.model

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentIntentTest {

    @Test
    fun parseIdFromClientSecret_parsesCorrectly() {
        val clientSecret = "pi_1CkiBMLENEVhOs7YMtUehLau_secret_s4O8SDh7s6spSmHDw1VaYPGZA"
        val paymentIntentId = PaymentIntent.ClientSecret(clientSecret).paymentIntentId
        assertEquals("pi_1CkiBMLENEVhOs7YMtUehLau", paymentIntentId)
    }

    @Test
    fun parsePaymentIntentWithPaymentMethods() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2
        assertTrue(paymentIntent.requiresAction())
        assertEquals("card", paymentIntent.paymentMethodTypes[0])
        assertEquals(0, paymentIntent.canceledAt)
        assertEquals(PaymentIntent.CaptureMethod.Automatic, paymentIntent.captureMethod)
        assertEquals(PaymentIntent.ConfirmationMethod.Automatic, paymentIntent.confirmationMethod)
        assertNotNull(paymentIntent.nextAction)
        assertEquals("jenny@example.com", paymentIntent.receiptEmail)
        assertNull(paymentIntent.cancellationReason)
    }

    @Test
    fun getNextActionData_whenUseStripeSdkWith3ds2() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2
        assertTrue(paymentIntent.nextActionData is StripeIntent.NextActionData.SdkData.Use3DS2)
        val sdkData = paymentIntent.nextActionData as StripeIntent.NextActionData.SdkData.Use3DS2
        assertEquals("mastercard", sdkData.serverName)
    }

    @Test
    fun getNextActionData_whenUseStripeSdkWith3ds1() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_3DS1
        assertTrue(paymentIntent.nextActionData is StripeIntent.NextActionData.SdkData.Use3DS1)
        val sdkData = paymentIntent.nextActionData as StripeIntent.NextActionData.SdkData.Use3DS1
        assertThat(sdkData.url).isNotEmpty()
    }

    @Test
    fun getNextActionData_whenRedirectToUrl() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_REDIRECT
        assertTrue(paymentIntent.nextActionData is StripeIntent.NextActionData.RedirectToUrl)
        val redirectData = paymentIntent.nextActionData as StripeIntent.NextActionData.RedirectToUrl
        assertEquals(Uri.parse("https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecaz6CRMbs6FrXfuYKBRSUG/src_client_secret_F6octeOshkgxT47dr0ZxSZiv"),
            redirectData.url)
        assertEquals(redirectData.returnUrl, "stripe://deeplink")
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

    @Test
    fun clientSecret_withInvalidKeys_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            PaymentIntent.ClientSecret("pi_12345")
        }

        assertFailsWith<IllegalArgumentException> {
            PaymentIntent.ClientSecret("pi_12345_secret_")
        }

        assertFailsWith<IllegalArgumentException> {
            SetupIntent.ClientSecret("pi_secret")
        }

        assertFailsWith<IllegalArgumentException> {
            SetupIntent.ClientSecret("pi_secret_a")
        }

        assertFailsWith<IllegalArgumentException> {
            PaymentIntent.ClientSecret("pi_a1b2c3_secret_x7y8z9pi_a1b2c3_secret_x7y8z9")
        }
    }

    @Test
    fun clientSecret_withValidKeys_succeeds() {
        assertEquals(
            "pi_a1b2c3_secret_x7y8z9",
            PaymentIntent.ClientSecret("pi_a1b2c3_secret_x7y8z9").value
        )
    }
}
