package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class CheckoutAnalyticsPerformerTest {

    @Test
    fun `payment element confirmation reports success and clears saved context`() = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val eventReporter = FakeEventReporter()
        val savedStateHandle = SavedStateHandle()
        val performer = createPerformer(confirmationHandler, eventReporter, savedStateHandle)
        val selection = PaymentSelection.GooglePay

        performer.onPaymentElementConfirmationStarted(selection)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            performer.reportConfirmationResults()
        }
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successResult)

        val call = eventReporter.paymentSuccessCalls.awaitItem()
        assertThat(call.paymentSelection).isEqualTo(selection)
        assertThat(call.intentId).isEqualTo(successResult.intent.id)

        confirmationHandler.state.value = ConfirmationHandler.State.Idle
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successResult)
        runCurrent()

        confirmationHandler.validate()
        eventReporter.validate()
    }

    @Test
    fun `express checkout confirmation context survives performer recreation`() = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val eventReporter = FakeEventReporter()
        val savedStateHandle = SavedStateHandle()
        val selection = PaymentSelection.Link(brand = LinkBrand.Link)

        createPerformer(confirmationHandler, eventReporter, savedStateHandle)
            .onExpressCheckoutElementConfirmationStarted(selection)
        val restoredPerformer = createPerformer(confirmationHandler, eventReporter, savedStateHandle)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            restoredPerformer.reportConfirmationResults()
        }
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successResult)

        assertThat(eventReporter.paymentSuccessCalls.awaitItem().paymentSelection).isEqualTo(selection)

        confirmationHandler.validate()
        eventReporter.validate()
    }

    private fun createPerformer(
        confirmationHandler: ConfirmationHandler,
        eventReporter: FakeEventReporter,
        savedStateHandle: SavedStateHandle,
    ): CheckoutAnalyticsPerformer {
        return CheckoutAnalyticsPerformer(
            confirmationHandler = confirmationHandler,
            eventReporter = eventReporter,
            savedStateHandle = savedStateHandle,
        )
    }

    private companion object {
        val successResult = ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
    }
}
