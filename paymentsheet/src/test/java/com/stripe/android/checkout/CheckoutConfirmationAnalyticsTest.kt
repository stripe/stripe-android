@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.analytics.CheckoutEventReporterFactory
import com.stripe.android.core.Logger
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.confirmation.CONFIRMATION_PARAMETERS
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CheckoutConfirmationAnalyticsTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `completion gate reports one terminal after refresh despite competing failure`() = runScenario {
        val releaseRefresh = CompletableDeferred<Unit>()
        sessionRefresher.enqueueRefreshAction { releaseRefresh.await() }
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
        )
        sessionRefresher.calls.awaitItem()

        coordinator.failConfirmation(IllegalStateException("competing failure"))
        assertThat(events()).containsExactly("checkout_elements.confirm.started")

        releaseRefresh.complete(Unit)
        results.awaitItem()
        eventReporter.paymentSuccessCalls.awaitItem()
        confirmationHandler.state.value = ConfirmationHandler.State.Idle
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
        )

        assertThat(events()).containsExactly(
            "checkout_elements.confirm.started",
            "checkout_elements.confirm.succeeded",
        ).inOrder()
    }

    @Test
    fun `admitted startup failure reports through coordinator`() = runScenario {
        val error = IllegalStateException("start failed")
        sessionRefresher.enqueueRefreshAction {}

        coordinator.failConfirmation(error)

        sessionRefresher.calls.awaitItem()
        assertThat((results.awaitItem() as CheckoutController.Result.Failed).error).isSameInstanceAs(error)
        assertThat(events()).containsExactly(
            "checkout_elements.confirm.started",
            "checkout_elements.confirm.failed",
        ).inOrder()
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val savedStateHandle = SavedStateHandle()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(savedStateHandle).apply {
            state = CheckoutControllerStateFactory.create()
        }
        val executor = FakeAnalyticsRequestExecutor()
        val eventReporter = FakeEventReporter()
        val analyticsPerformer = CheckoutConfirmationAnalyticsPerformerFactory.create(
            eventReporter = eventReporter,
            savedStateHandle = savedStateHandle,
            stateHolder = stateHolder,
            checkoutEventReporter = CheckoutEventReporterFactory.create(executor),
        )
        val confirmationHandler = FakeConfirmationHandler()
        val sessionRefresher = FakeCheckoutSessionRefresher()
        val results = Turbine<CheckoutController.Result>()
        val coordinator = CheckoutOperationCoordinator(
            confirmationHandler = confirmationHandler,
            sheetStateHolder = SheetStateHolder(savedStateHandle),
            sessionRefresher = sessionRefresher,
            analyticsPerformer = analyticsPerformer,
            logger = Logger.noop(),
            resultCallback = CheckoutController.ResultCallback(results::add),
            viewModelScope = backgroundScope,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            coordinator.observeConfirmationResults()
        }
        assertThat(coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }).isNotNull()
        analyticsPerformer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)

        Scenario(coordinator, confirmationHandler, sessionRefresher, results, eventReporter, executor).block()

        confirmationHandler.validate()
        sessionRefresher.ensureAllEventsConsumed()
        results.ensureAllEventsConsumed()
        eventReporter.validate()
    }

    private class Scenario(
        val coordinator: CheckoutOperationCoordinator,
        val confirmationHandler: FakeConfirmationHandler,
        val sessionRefresher: FakeCheckoutSessionRefresher,
        val results: Turbine<CheckoutController.Result>,
        val eventReporter: FakeEventReporter,
        private val executor: FakeAnalyticsRequestExecutor,
    ) {
        fun events(): List<Any?> = executor.getExecutedRequests().map { it.params["event"] }
    }
}
