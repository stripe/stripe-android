package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class CheckoutAnalyticsPerformerTest {

    @Test
    fun `payment element confirmation reports success and clears saved context`() = runScenario {
        val selection = PaymentSelection.GooglePay

        performer.onPaymentElementConfirmationStarted(selection)
        reportConfirmationResults(performer)
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successResult)

        val call = eventReporter.paymentSuccessCalls.awaitItem()
        assertThat(call.paymentSelection).isEqualTo(selection)
        assertThat(call.intentId).isEqualTo(successResult.intent.id)

        confirmationHandler.state.value = ConfirmationHandler.State.Idle
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successResult)
        runCurrent()
    }

    @Test
    fun `express checkout confirmation context survives performer recreation`() = runScenario {
        val selection = PaymentSelection.Link(brand = LinkBrand.Link)

        performer.onExpressCheckoutElementConfirmationStarted(selection)
        val restoredPerformer = recreatePerformer()
        reportConfirmationResults(restoredPerformer)
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successResult)

        assertThat(eventReporter.paymentSuccessCalls.awaitItem().paymentSelection).isEqualTo(selection)
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val eventReporter = FakeEventReporter()
        val savedStateHandle = SavedStateHandle()
        val performer = CheckoutAnalyticsPerformer(
            confirmationHandler = confirmationHandler,
            eventReporter = eventReporter,
            savedStateHandle = savedStateHandle,
        )

        Scenario(
            performer = performer,
            confirmationHandler = confirmationHandler,
            eventReporter = eventReporter,
            savedStateHandle = savedStateHandle,
            backgroundScope = backgroundScope,
            testDispatcher = UnconfinedTestDispatcher(testScheduler),
        ).block()

        confirmationHandler.validate()
        eventReporter.validate()
    }

    private class Scenario(
        val performer: CheckoutAnalyticsPerformer,
        val confirmationHandler: FakeConfirmationHandler,
        val eventReporter: FakeEventReporter,
        private val savedStateHandle: SavedStateHandle,
        private val backgroundScope: CoroutineScope,
        private val testDispatcher: TestDispatcher,
    ) {
        fun recreatePerformer(): CheckoutAnalyticsPerformer {
            return CheckoutAnalyticsPerformer(
                confirmationHandler = confirmationHandler,
                eventReporter = eventReporter,
                savedStateHandle = savedStateHandle,
            )
        }

        fun reportConfirmationResults(performer: CheckoutAnalyticsPerformer) {
            backgroundScope.launch(testDispatcher) {
                performer.reportConfirmationResults()
            }
        }

        fun runCurrent() {
            testDispatcher.scheduler.runCurrent()
        }
    }

    private companion object {
        val successResult = ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
    }
}
