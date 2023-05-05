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
    fun parsePaymentIntentWith3DS2PaymentMethods() {
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
        assertThat(paymentIntent.nextActionData)
            .isNotNull()
        assertThat(paymentIntent.receiptEmail)
            .isEqualTo("jenny@example.com")
        assertThat(paymentIntent.cancellationReason)
            .isNull()
    }

    @Test
    fun parsePaymentIntentWithBlikPaymentMethods() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_BLIK_AUTHORIZE
        assertThat(paymentIntent.requiresAction())
            .isTrue()
        assertThat(paymentIntent.paymentMethodTypes)
            .containsExactly("blik")
    }

    @Test
    fun parsePaymentIntentWithWeChatPayPaymentMethods() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_WECHAT_PAY_AUTHORIZE
        assertThat(paymentIntent.requiresAction())
            .isTrue()
        assertThat(paymentIntent.paymentMethodTypes)
            .containsExactly("wechat_pay")
    }

    @Test
    fun parsePaymentIntentWithKlarnaPaymentMethods() {
        val paymentIntent = PaymentIntentFixtures.PI_WITH_KLARNA_IN_PAYMENT_METHODS
        assertThat(paymentIntent.paymentMethodTypes)
            .containsExactly("klarna")
    }

    @Test
    fun parsePaymentIntentWithAffirmPaymentMethods() {
        val paymentIntent = PaymentIntentFixtures.PI_WITH_AFFIRM_IN_PAYMENT_METHODS
        assertThat(paymentIntent.paymentMethodTypes)
            .containsExactly("affirm")
    }

    @Test
    fun parsePaymentIntentWithUSBankAccountPaymentMethods() {
        val paymentIntent = PaymentIntentFixtures.PI_WITH_US_BANK_ACCOUNT_IN_PAYMENT_METHODS
        assertThat(paymentIntent.paymentMethodTypes)
            .containsExactly("us_bank_account")
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
                Uri.parse(
                    "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecaz6CR" +
                        "Mbs6FrXfuYKBRSUG/src_client_secret_F6octeOshkgxT47dr0ZxSZiv"
                )
            )
        assertThat(redirectData.returnUrl)
            .isEqualTo("stripe://deeplink")
    }

    @Test
    fun getNextActionData_whenBlikAuthorize() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_BLIK_AUTHORIZE
        assertThat(paymentIntent.nextActionData)
            .isInstanceOf(StripeIntent.NextActionData.BlikAuthorize::class.java)
    }

    @Test
    fun getNextActionData_whenWeChatPay() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_WECHAT_PAY_AUTHORIZE
        assertThat(paymentIntent.nextActionData)
            .isInstanceOf(StripeIntent.NextActionData.WeChatPayRedirect::class.java)
        val weChat =
            (paymentIntent.nextActionData as StripeIntent.NextActionData.WeChatPayRedirect).weChat
        assertThat(weChat).isEqualTo(
            WeChat(
                appId = "wx65997d6307c3827d",
                nonce = "some_random_string",
                packageValue = "Sign=WXPay",
                partnerId = "wx65997d6307c3827d",
                prepayId = "test_transaction",
                timestamp = "1619638941",
                sign = "8B26124BABC816D7140034DDDC7D3B2F1036CCB2D910E52592687F6A44790D5E"
            )
        )
    }

    @Test
    fun getNextActionData_whenVerifyWithMicrodeposits() {
        val paymentIntent = PaymentIntentFixtures.PI_WITH_US_BANK_ACCOUNT_IN_PAYMENT_METHODS
        assertThat(paymentIntent.nextActionData)
            .isInstanceOf(StripeIntent.NextActionData.VerifyWithMicrodeposits::class.java)
        val verify =
            (paymentIntent.nextActionData as StripeIntent.NextActionData.VerifyWithMicrodeposits)
        assertThat(verify).isEqualTo(
            StripeIntent.NextActionData.VerifyWithMicrodeposits(
                arrivalDate = 1647241200,
                hostedVerificationUrl = "https://payments.stripe.com/microdeposit/pacs_test_YWNjdF8" +
                    "xS2J1SjlGbmt1bWlGVUZ4LHBhX25vbmNlX0xJcFVEaERaU0JOVVR3akhxMXc5eklOQkl3UTlwNWo00" +
                    "00v3GS1Jej",
                microdepositType = MicrodepositType.AMOUNTS
            )
        )
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
                "The provided PaymentMethod has failed authentication. You can provide " +
                    "payment_method_data or a new PaymentMethod to attempt to fulfill this " +
                    "PaymentIntent again."
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

    @Test
    fun `Determines SFU correctly if it's set on the intent itself`() {
        val offSession = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            setupFutureUsage = StripeIntent.Usage.OffSession,
        )
        assertThat(offSession.isSetupFutureUsageSet("card")).isTrue()

        val onSession = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            setupFutureUsage = StripeIntent.Usage.OnSession,
        )
        assertThat(onSession.isSetupFutureUsageSet("card")).isTrue()

        val oneTime = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            setupFutureUsage = StripeIntent.Usage.OneTime,
        )
        assertThat(oneTime.isSetupFutureUsageSet("card")).isFalse()

        val none = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            setupFutureUsage = null,
        )
        assertThat(none.isSetupFutureUsageSet("card")).isFalse()
    }

    @Test
    fun `Determines SFU correctly if setup_future_usage exists in payment method options`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodOptionsJsonString = """
                {
                  "card": {
                    "setup_future_usage": ""
                  }
                }
            """.trimIndent()
        )

        val result = paymentIntent.isSetupFutureUsageSet("card")
        assertThat(result).isTrue()
    }

    @Test
    fun `Determines SFU correctly if setup_future_usage does not exist in payment method options`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodOptionsJsonString = """
                {
                  "card": {
                    "some_other_key_that_has_nothing_to_do_with_sfu": ""
                  }
                }
            """.trimIndent()
        )

        val result = paymentIntent.isSetupFutureUsageSet("card")
        assertThat(result).isFalse()
    }

    @Test
    fun `getPaymentMethodOptions returns expected results`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodOptionsJsonString = """
                {
                    "card": {
                        "mandate_options": null,
                        "network": null,
                        "request_three_d_secure": "automatic"
                    },
                    "us_bank_account": {
                        "financial_connections": {
                            "permissions": "balances"
                        },
                        "setup_future_usage": "on_session",
                        "verification_method": "automatic"
                    }
                }
            """.trimIndent()
        )

        assertThat(paymentIntent.getPaymentMethodOptions())
            .isEqualTo(
                mapOf(
                    "card" to mapOf(
                        "mandate_options" to null,
                        "network" to null,
                        "request_three_d_secure" to "automatic"
                    ),
                    "us_bank_account" to mapOf(
                        "financial_connections" to mapOf(
                            "permissions" to "balances"
                        ),
                        "setup_future_usage" to "on_session",
                        "verification_method" to "automatic"
                    )
                )
            )
    }
}
