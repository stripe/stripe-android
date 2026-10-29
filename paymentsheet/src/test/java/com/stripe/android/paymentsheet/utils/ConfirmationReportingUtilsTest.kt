package com.stripe.android.paymentsheet.utils

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.MutableConfirmationMetadata
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationTypeKey
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ConfirmationReportingUtilsTest {

    @Test
    fun `reportPaymentResult forwards transaction failure without analytics overrides`() = runTest {
        val eventReporter = FakeEventReporter()
        val result = ConfirmationHandler.Result.Failed(
            cause = IllegalStateException("Payment failed"),
            message = "Something went wrong".resolvableString,
            type = ConfirmationHandler.Result.Failed.ErrorType.Transaction(),
        )

        eventReporter.reportPaymentResult(result, PaymentSelection.GooglePay)

        val event = eventReporter.paymentFailureCalls.awaitItem()
        assertThat(event.error.cause).isSameInstanceAs(result.cause)
        assertThat(event.error.analyticsValue).isNull()
        assertThat(event.error.errorCode).isNull()
        eventReporter.validate()
    }

    @Test
    fun `reportPaymentResult forwards provided payment method analytics value`() = runTest {
        val eventReporter = FakeEventReporter()
        val result = ConfirmationHandler.Result.Failed(
            cause = IllegalStateException("External payment failed"),
            message = "Something went wrong".resolvableString,
            type = ConfirmationHandler.Result.Failed.ErrorType.Transaction(
                analyticsValue = "externalPaymentMethodError",
            ),
        )

        eventReporter.reportPaymentResult(result, PaymentSelection.GooglePay)

        val event = eventReporter.paymentFailureCalls.awaitItem()
        assertThat(event.error.cause).isSameInstanceAs(result.cause)
        assertThat(event.error.analyticsValue).isEqualTo("externalPaymentMethodError")
        assertThat(event.error.errorCode).isNull()
        eventReporter.validate()
    }

    @Test
    fun `reportPaymentResult does not report internal failure`() = assertFailureNotReported(
        ConfirmationHandler.Result.Failed.ErrorType.Internal,
    )

    @Test
    fun `reportPaymentResult does not report merchant integration failure`() = assertFailureNotReported(
        ConfirmationHandler.Result.Failed.ErrorType.MerchantIntegration,
    )

    @Test
    fun `reportPaymentResult does not report fatal failure`() = assertFailureNotReported(
        ConfirmationHandler.Result.Failed.ErrorType.Fatal,
    )

    @Test
    fun `reportPaymentResult reports success correctly`() = runTest {
        val eventReporter = FakeEventReporter()
        val result = ConfirmationHandler.Result.Succeeded(
            intent = PaymentIntentFixtures.PI_SUCCEEDED,
            metadata = MutableConfirmationMetadata().apply {
                set(DeferredIntentConfirmationTypeKey, DeferredIntentConfirmationType.Client)
            },
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
            type = ConfirmationHandler.Result.Failed.ErrorType.Transaction(
                analyticsValue = "googlePay_12",
                errorCode = "12",
            )
        )

        eventReporter.reportPaymentResult(result, PaymentSelection.GooglePay)

        val event = eventReporter.paymentFailureCalls.awaitItem()
        assertThat(event.paymentSelection).isEqualTo(PaymentSelection.GooglePay)
        assertThat(event.error.cause).isSameInstanceAs(result.cause)
        assertThat(event.error.analyticsValue).isEqualTo("googlePay_12")
        assertThat(event.error.errorCode).isEqualTo("12")
        eventReporter.validate()
    }

    private fun assertFailureNotReported(type: ConfirmationHandler.Result.Failed.ErrorType) = runTest {
        val eventReporter = FakeEventReporter()
        val result = ConfirmationHandler.Result.Failed(
            cause = IllegalStateException("Payment failed"),
            message = "Something went wrong".resolvableString,
            type = type,
        )

        eventReporter.reportPaymentResult(result, PaymentSelection.GooglePay)

        eventReporter.paymentFailureCalls.expectNoEvents()
        eventReporter.validate()
    }
}
