package com.stripe.android.model

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class PaymentIntentTest {

    @Test
    fun parseIdFromClientSecret_parsesCorrectly() {
        val clientSecret = "pi_1CkiBMLENEVhOs7YMtUehLau_secret_s4O8SDh7s6spSmHDw1VaYPGZA"
        val paymentIntentId = PaymentIntent.ClientSecret(clientSecret).paymentIntentId
        assertThat(paymentIntentId)
            .isEqualTo("pi_1CkiBMLENEVhOs7YMtUehLau")
    }

    @Test
    fun parsePaymentIntentWithPaymentMethods() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2
        assertThat(paymentIntent.requiresAction())
            .isTrue()
        assertThat(paymentIntent.paymentMethodTypes)
            .containsExactly("card")
        assertThat(paymentIntent.canceledAt)
            .isEqualTo(0)
        assertThat(paymentIntent.captureMethod)
            .isEqualTo(PaymentIntent.CaptureMethod.Automatic)
        assertThat(paymentIntent.confirmationMethod)
            .isEqualTo(PaymentIntent.ConfirmationMethod.Manual)
        assertThat(paymentIntent.nextAction)
            .isNotNull()
        assertThat(paymentIntent.receiptEmail)
            .isEqualTo("jenny@example.com")
        assertThat(paymentIntent.cancellationReason)
            .isNull()
    }

    @Test
    fun getNextActionData_whenUseStripeSdkWith3ds2() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2
        assertThat(paymentIntent.nextActionData)
            .isInstanceOf(StripeIntent.NextActionData.SdkData.Use3DS2::class.java)
        val sdkData =
            paymentIntent.nextActionData as StripeIntent.NextActionData.SdkData.Use3DS2
        assertThat(sdkData.serverName)
            .isEqualTo("mastercard")
    }

    @Test
    fun getNextActionData_whenUseStripeSdkWith3ds1() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_3DS1
        assertThat(paymentIntent.nextActionData)
            .isInstanceOf(StripeIntent.NextActionData.SdkData.Use3DS1::class.java)
        val sdkData = paymentIntent.nextActionData as StripeIntent.NextActionData.SdkData.Use3DS1
        assertThat(sdkData.url)
            .isNotEmpty()
    }

    @Test
    fun getNextActionData_whenRedirectToUrl() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_REDIRECT
        assertThat(paymentIntent.nextActionData)
            .isInstanceOf(StripeIntent.NextActionData.RedirectToUrl::class.java)
        val redirectData = paymentIntent.nextActionData as StripeIntent.NextActionData.RedirectToUrl
        assertThat(redirectData.url)
            .isEqualTo(
                Uri.parse("https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecaz6CRMbs6FrXfuYKBRSUG/src_client_secret_F6octeOshkgxT47dr0ZxSZiv")
            )
        assertThat(redirectData.returnUrl)
            .isEqualTo("stripe://deeplink")
    }

    @Test
    fun getLastPaymentError_parsesCorrectly() {
        val lastPaymentError =
            requireNotNull(PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR.lastPaymentError)
        assertThat(lastPaymentError.paymentMethod?.id)
            .isEqualTo("pm_1F7J1bCRMbs6FrXfQKsYwO3U")
        assertThat(lastPaymentError.code)
            .isEqualTo("payment_intent_authentication_failure")
        assertThat(lastPaymentError.type)
            .isEqualTo(PaymentIntent.Error.Type.InvalidRequestError)
        assertThat(lastPaymentError.docUrl)
            .isEqualTo("https://stripe.com/docs/error-codes/payment-intent-authentication-failure")
        assertThat(lastPaymentError.message)
            .isEqualTo(
                "The provided PaymentMethod has failed authentication. You can provide payment_method_data or a new PaymentMethod to attempt to fulfill this PaymentIntent again."
            )
    }

    @Test
    fun testCanceled() {
        assertThat(PaymentIntentFixtures.CANCELLED.status)
            .isEqualTo(StripeIntent.Status.Canceled)
        assertThat(PaymentIntentFixtures.CANCELLED.cancellationReason)
            .isEquivalentAccordingToCompareTo(PaymentIntent.CancellationReason.Abandoned)
        assertThat(PaymentIntentFixtures.CANCELLED.canceledAt)
            .isEqualTo(1567091866L)
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
        assertThat(PaymentIntent.ClientSecret("pi_a1b2c3_secret_x7y8z9").value)
            .isEqualTo("pi_a1b2c3_secret_x7y8z9")
    }
}
