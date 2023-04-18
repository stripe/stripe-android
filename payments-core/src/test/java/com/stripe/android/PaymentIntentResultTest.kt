package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.LuxePostConfirmActionRepository
import com.stripe.android.model.MicrodepositType
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.utils.ParcelUtils
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentIntentResultTest {

    @Test
    fun `test outcome checks lpm repository first`() {
        val paymentIntent = PaymentIntent(
            created = 500L,
            amount = 1000L,
            clientSecret = "secret",
            paymentMethod = PaymentMethodFixtures.OXXO_PAYMENT_METHOD,
            isLiveMode = false,
            id = "pi_12345",
            currency = "usd",
            countryCode = null,
            paymentMethodTypes = listOf(PaymentMethod.Type.Oxxo.code),
            status = StripeIntent.Status.Processing,
            unactivatedPaymentMethods = emptyList(),
            nextActionData = StripeIntent.NextActionData.DisplayOxxoDetails()
        )
        val result = PaymentIntentResult(
            intent = paymentIntent
        )
        result.luxePostConfirmActionRepository = LuxePostConfirmActionRepository()

        // Because of the OXXO test below we know this normally returns SUCCESS

        // We will change the status from Success to Cancelled when in the processing state
        result.luxePostConfirmActionRepository.update(
            mapOf(
                "oxxo" to LUXE_NEXT_ACTION.copy(
                    postConfirmActionIntentStatus = mapOf(
                        StripeIntent.Status.Processing to StripeIntentResult.Outcome.CANCELED
                    )
                )
            )
        )

        assertThat(result.outcome)
            .isEqualTo(StripeIntentResult.Outcome.CANCELED)
    }

    @Test
    fun `intent should return expected object`() {
        assertThat(PaymentIntentResult(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2).intent)
            .isEqualTo(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2)
    }

    @Test
    fun outcome_whenBacsAndProcessing_shouldReturnSucceeded() {
        val paymentIntent = PaymentIntent(
            created = 500L,
            amount = 1000L,
            clientSecret = "secret",
            paymentMethod = PaymentMethodFixtures.BACS_DEBIT_PAYMENT_METHOD,
            isLiveMode = false,
            id = "pi_12345",
            currency = "usd",
            countryCode = null,
            paymentMethodTypes = listOf("card"),
            status = StripeIntent.Status.Processing,
            unactivatedPaymentMethods = emptyList()
        )
        val result = PaymentIntentResult(
            intent = paymentIntent
        )
        assertThat(result.outcome)
            .isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
    }

    @Test
    fun outcome_whenOxxoAndProcessing_shouldReturnSucceeded() {
        val paymentIntent = PaymentIntent(
            created = 500L,
            amount = 1000L,
            clientSecret = "secret",
            paymentMethod = PaymentMethodFixtures.OXXO_PAYMENT_METHOD,
            isLiveMode = false,
            id = "pi_12345",
            currency = "usd",
            countryCode = null,
            paymentMethodTypes = listOf(PaymentMethod.Type.Oxxo.code),
            status = StripeIntent.Status.Processing,
            unactivatedPaymentMethods = emptyList(),
            nextActionData = StripeIntent.NextActionData.DisplayOxxoDetails()
        )
        val result = PaymentIntentResult(
            intent = paymentIntent
        )
        assertThat(result.outcome)
            .isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
    }

    @Test
    fun outcome_whenUSBankAccountAndRequiresAction_shouldReturnSucceeded() {
        val paymentIntent = PaymentIntent(
            created = 500L,
            amount = 1000L,
            clientSecret = "secret",
            paymentMethod = PaymentMethodFixtures.US_BANK_ACCOUNT_PAYMENT_METHOD,
            isLiveMode = false,
            id = "pi_12345",
            currency = "usd",
            countryCode = null,
            paymentMethodTypes = listOf(PaymentMethod.Type.USBankAccount.code),
            status = StripeIntent.Status.RequiresAction,
            unactivatedPaymentMethods = emptyList(),
            nextActionData = StripeIntent.NextActionData.VerifyWithMicrodeposits(
                arrivalDate = 1234567,
                hostedVerificationUrl = "hostedVerificationUrl",
                microdepositType = MicrodepositType.AMOUNTS
            )
        )
        val result = PaymentIntentResult(
            intent = paymentIntent
        )
        assertThat(result.outcome)
            .isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
    }

    @Test
    fun outcome_whenCardAndProcessing_shouldReturnUnknown() {
        val paymentIntent = PaymentIntent(
            created = 500L,
            amount = 1000L,
            clientSecret = "secret",
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            isLiveMode = false,
            id = "pi_12345",
            currency = "usd",
            countryCode = null,
            paymentMethodTypes = listOf("card"),
            status = StripeIntent.Status.Processing,
            unactivatedPaymentMethods = emptyList()
        )
        val result = PaymentIntentResult(
            intent = paymentIntent
        )
        assertThat(result.outcome)
            .isEqualTo(StripeIntentResult.Outcome.UNKNOWN)
    }

    @Test
    fun outcome_whenBlikAndRequiresAction_shouldReturnSuccess() {
        val paymentIntent = PaymentIntent(
            created = 500L,
            amount = 1000L,
            clientSecret = "secret",
            paymentMethod = PaymentMethodFixtures.BLIK_PAYMENT_METHOD,
            isLiveMode = false,
            id = "pi_12345",
            currency = "pln",
            countryCode = "pl",
            paymentMethodTypes = listOf("blik"),
            status = StripeIntent.Status.RequiresAction,
            unactivatedPaymentMethods = emptyList(),
            nextActionData = StripeIntent.NextActionData.BlikAuthorize,
        )
        val result = PaymentIntentResult(
            intent = paymentIntent
        )
        assertThat(result.outcome)
            .isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
    }

    @Test
    fun `should parcelize correctly`() {
        ParcelUtils.verifyParcelRoundtrip(
            PaymentIntentResult(
                intent = PaymentIntentFixtures.PI_REQUIRES_AMEX_3DS2,
                outcomeFromFlow = StripeIntentResult.Outcome.CANCELED
            )
        )
    }
}
