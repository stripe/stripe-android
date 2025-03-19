package com.stripe.android.paymentsheet.utils

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ConfirmationReportingUtilsTest {

    @Test
    fun `toConfirmationError returns correct error for external payment method error`() {
        val epmError = ConfirmationHandler.Result.Failed(
            cause = Exception(),
            message = "Something went wrong".resolvableString,
            type = ConfirmationHandler.Result.Failed.ErrorType.ExternalPaymentMethod
        )

        val confirmationError = epmError.toConfirmationError()
        assertThat(confirmationError).isNotNull()
        assertThat(confirmationError).isInstanceOf<PaymentSheetConfirmationError.ExternalPaymentMethod>()
    }

    @Test
    fun `toConfirmationError returns correct error for stripe error`() {
        val stripeError = ConfirmationHandler.Result.Failed(
            cause = StripeException.create(
                Throwable(
                    message = "Something went wrong with Stripe",
                )
            ),
            message = "Something went wrong".resolvableString,
            type = ConfirmationHandler.Result.Failed.ErrorType.Payment
        )

        val confirmationError = stripeError.toConfirmationError()
        assertThat(confirmationError).isNotNull()
        assertThat(confirmationError).isInstanceOf<PaymentSheetConfirmationError.Stripe>()
        assertThat(confirmationError?.cause?.message).isEqualTo("Something went wrong with Stripe")
    }

    @Test
    fun `toConfirmationError returns correct error for google pay error`() {
        val googlePayError = ConfirmationHandler.Result.Failed(
            cause = Exception(),
            message = "Something went wrong".resolvableString,
            type = ConfirmationHandler.Result.Failed.ErrorType.GooglePay(12)
        )

        val confirmationError = googlePayError.toConfirmationError()
        assertThat(confirmationError).isNotNull()
        assertThat(confirmationError).isInstanceOf<PaymentSheetConfirmationError.GooglePay>()
        assertThat(confirmationError?.errorCode).isEqualTo("12")
    }

    @Test
    fun `toConfirmationError returns null for non reportable error`() {
        val merchantError = ConfirmationHandler.Result.Failed(
            cause = Exception(),
            message = "Something went wrong".resolvableString,
            type = ConfirmationHandler.Result.Failed.ErrorType.MerchantIntegration
        )

        val confirmationError = merchantError.toConfirmationError()
        assertThat(confirmationError).isNull()
    }

    @Test
    fun `reportPaymentResult reports success correctly`() = runTest {
        val eventReporter = FakeEventReporter()
        val result = ConfirmationHandler.Result.Succeeded(
            intent = PaymentIntentFixtures.PI_SUCCEEDED,
            deferredIntentConfirmationType = DeferredIntentConfirmationType.Client
        )

        eventReporter.reportPaymentResult(result, PaymentSelection.GooglePay)

        val event = eventReporter.paymentSuccessCalls.awaitItem()
        assertThat(event.paymentSelection).isEqualTo(PaymentSelection.GooglePay)
        assertThat(event.deferredIntentConfirmationType).isEqualTo(DeferredIntentConfirmationType.Client)
        eventReporter.validate()
    }

    @Test
    fun `reportPaymentResult reports failure correctly`() = runTest {
        val eventReporter = FakeEventReporter()
        val result = ConfirmationHandler.Result.Failed(
            cause = Exception(),
            message = "Something went wrong".resolvableString,
            type = ConfirmationHandler.Result.Failed.ErrorType.GooglePay(12)
        )

        eventReporter.reportPaymentResult(result, PaymentSelection.GooglePay)

        val event = eventReporter.paymentFailureCalls.awaitItem()
        assertThat(event.paymentSelection).isEqualTo(PaymentSelection.GooglePay)
        assertThat(event.error).isInstanceOf<PaymentSheetConfirmationError.GooglePay>()
        eventReporter.validate()
    }
}
