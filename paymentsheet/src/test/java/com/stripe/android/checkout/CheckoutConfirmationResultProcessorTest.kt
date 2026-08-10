package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutConfirmationResultProcessorTest {

    @Test
    fun `success clears state and returns completed`() = runScenario {
        val result = processor.process(
            result = ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED),
            confirmationWasRestored = false,
        )

        assertThat(result).isInstanceOf(CheckoutController.Result.Completed::class.java)
        clearCalls.awaitItem()
        assertThat(stateHolder.state).isNull()
        fetchCalls.expectNoEvents()
        reloadCalls.expectNoEvents()
    }

    @Test
    fun `failure refreshes state and preserves its cause`() {
        val error = IllegalStateException("Confirmation failed")
        val refreshedResponse = response(id = "cs_refreshed")

        runScenario(fetchResult = Result.success(refreshedResponse)) {
            val result = processor.process(
                result = failedResult(error),
                confirmationWasRestored = false,
            )

            assertThat((result as CheckoutController.Result.Failed).error).isSameInstanceAs(error)
            assertThat(fetchCalls.awaitItem()).isEqualTo(expectedFetchCall())
            assertThat(reloadCalls.awaitItem().checkoutSessionResponse).isEqualTo(refreshedResponse)
            clearCalls.expectNoEvents()
        }
    }

    @Test
    fun `informed cancellation refreshes state and returns canceled`() = runScenario(
        fetchResult = Result.success(response()),
    ) {
        val result = processor.process(
            result = canceledResult(ConfirmationHandler.Result.Canceled.Action.InformCancellation),
            confirmationWasRestored = false,
        )

        assertThat(result).isInstanceOf(CheckoutController.Result.Canceled::class.java)
        fetchCalls.awaitItem()
        reloadCalls.awaitItem()
    }

    @Test
    fun `none cancellation refreshes state without returning a result`() = runScenario(
        fetchResult = Result.success(response()),
    ) {
        val result = processor.process(
            result = canceledResult(ConfirmationHandler.Result.Canceled.Action.None),
            confirmationWasRestored = false,
        )

        assertThat(result).isNull()
        fetchCalls.awaitItem()
        reloadCalls.awaitItem()
    }

    @Test
    fun `restored none cancellation returns canceled`() = runScenario(
        fetchResult = Result.success(response()),
    ) {
        val result = processor.process(
            result = canceledResult(ConfirmationHandler.Result.Canceled.Action.None),
            confirmationWasRestored = true,
        )

        assertThat(result).isInstanceOf(CheckoutController.Result.Canceled::class.java)
        fetchCalls.awaitItem()
        reloadCalls.awaitItem()
    }

    @Test
    fun `modify payment details refreshes state without returning a result`() = runScenario(
        fetchResult = Result.success(response()),
    ) {
        val result = processor.process(
            result = canceledResult(ConfirmationHandler.Result.Canceled.Action.ModifyPaymentDetails),
            confirmationWasRestored = false,
        )

        assertThat(result).isNull()
        fetchCalls.awaitItem()
        reloadCalls.awaitItem()
    }

    @Test
    fun `start failure refreshes state and preserves its cause`() {
        val error = IllegalStateException("Start failed")

        runScenario(fetchResult = Result.success(response())) {
            val result = processor.processFailure(error)

            assertThat(result.error).isSameInstanceAs(error)
            fetchCalls.awaitItem()
            reloadCalls.awaitItem()
        }
    }

    @Test
    fun `failed refresh leaves state unchanged and preserves confirmation result`() {
        val error = IllegalStateException("Confirmation failed")

        runScenario(fetchResult = Result.failure(IllegalStateException("Refresh failed"))) {
            val result = processor.process(
                result = failedResult(error),
                confirmationWasRestored = false,
            )

            assertThat((result as CheckoutController.Result.Failed).error).isSameInstanceAs(error)
            fetchCalls.awaitItem()
            reloadCalls.expectNoEvents()
            assertThat(stateHolder.state).isEqualTo(initialState)
        }
    }

    @Test
    fun `thrown refresh failure leaves state unchanged and preserves confirmation result`() {
        val error = IllegalStateException("Confirmation failed")

        runScenario(fetchError = IllegalStateException("Refresh failed")) {
            val result = processor.process(
                result = failedResult(error),
                confirmationWasRestored = false,
            )

            assertThat((result as CheckoutController.Result.Failed).error).isSameInstanceAs(error)
            fetchCalls.awaitItem()
            reloadCalls.expectNoEvents()
            assertThat(stateHolder.state).isEqualTo(initialState)
        }
    }

    @Test
    fun `fetch result cancellation is propagated`() {
        val cancellation = CancellationException("Canceled")

        runScenario(fetchResult = Result.failure(cancellation)) {
            val thrown = assertFailsWith<CancellationException> {
                processor.processFailure(IllegalStateException("Start failed"))
            }

            assertThat(thrown).isSameInstanceAs(cancellation)
            fetchCalls.awaitItem()
            reloadCalls.expectNoEvents()
        }
    }

    @Test
    fun `thrown fetch cancellation is propagated`() {
        val cancellation = CancellationException("Canceled")

        runScenario(fetchError = cancellation) {
            val thrown = assertFailsWith<CancellationException> {
                processor.processFailure(IllegalStateException("Start failed"))
            }

            assertThat(thrown).isSameInstanceAs(cancellation)
            fetchCalls.awaitItem()
            reloadCalls.expectNoEvents()
        }
    }

    @Test
    fun `reload failure leaves state unchanged and preserves confirmation result`() {
        val error = IllegalStateException("Confirmation failed")

        runScenario(
            fetchResult = Result.success(response(id = "cs_refreshed")),
            reloadError = IllegalStateException("Reload failed"),
        ) {
            val result = processor.process(
                result = failedResult(error),
                confirmationWasRestored = false,
            )

            assertThat((result as CheckoutController.Result.Failed).error).isSameInstanceAs(error)
            fetchCalls.awaitItem()
            reloadCalls.awaitItem()
            assertThat(stateHolder.state).isEqualTo(initialState)
        }
    }

    @Test
    fun `missing state skips refresh and preserves confirmation result`() = runScenario(state = null) {
        val error = IllegalStateException("Confirmation failed")

        val result = processor.process(
            result = failedResult(error),
            confirmationWasRestored = false,
        )

        assertThat((result as CheckoutController.Result.Failed).error).isSameInstanceAs(error)
        fetchCalls.expectNoEvents()
        reloadCalls.expectNoEvents()
    }

    private fun runScenario(
        state: CheckoutControllerState? = CheckoutControllerStateFactory.create(),
        fetchResult: Result<CheckoutSessionResponse> = Result.failure(
            IllegalStateException("Unexpected fetch")
        ),
        fetchError: Exception? = null,
        reloadError: Exception? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val fetchCalls = Turbine<FetchCall>()
        val reloadCalls = Turbine<CheckoutControllerState>()
        val clearCalls = Turbine<Unit>()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(SavedStateHandle()).apply {
            this.state = state
        }
        val processor = CheckoutConfirmationResultProcessor(
            stateHolder = stateHolder,
            fetchResponse = { sessionId, adaptivePricingAllowed ->
                fetchCalls.add(FetchCall(sessionId, adaptivePricingAllowed))
                fetchError?.let { throw it }
                fetchResult
            },
            reloadState = { updatedState ->
                reloadCalls.add(updatedState)
                reloadError?.let { throw it }
                stateHolder.state = updatedState
            },
            clearState = {
                clearCalls.add(Unit)
                stateHolder.state = null
            },
            logger = Logger.noop(),
        )

        Scenario(
            processor = processor,
            initialState = state,
            stateHolder = stateHolder,
            fetchCalls = fetchCalls,
            reloadCalls = reloadCalls,
            clearCalls = clearCalls,
        ).block()

        fetchCalls.ensureAllEventsConsumed()
        reloadCalls.ensureAllEventsConsumed()
        clearCalls.ensureAllEventsConsumed()
    }

    private fun failedResult(error: Throwable): ConfirmationHandler.Result.Failed {
        return ConfirmationHandler.Result.Failed(
            cause = error,
            message = "Confirmation failed".resolvableString,
            type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
        )
    }

    private fun canceledResult(
        action: ConfirmationHandler.Result.Canceled.Action,
    ): ConfirmationHandler.Result.Canceled {
        return ConfirmationHandler.Result.Canceled(action)
    }

    private fun response(id: String = "cs_test"): CheckoutSessionResponse {
        return CheckoutSessionResponseFactory.create(id = id)
    }

    private class Scenario(
        val processor: CheckoutConfirmationResultProcessor,
        val initialState: CheckoutControllerState?,
        val stateHolder: CheckoutControllerStateHolder,
        val fetchCalls: Turbine<FetchCall>,
        val reloadCalls: Turbine<CheckoutControllerState>,
        val clearCalls: Turbine<Unit>,
    ) {
        fun expectedFetchCall(): FetchCall {
            val state = requireNotNull(initialState)
            return FetchCall(
                sessionId = state.checkoutSessionResponse.id,
                adaptivePricingAllowed = state.configuration.adaptivePricingAllowed,
            )
        }
    }

    private data class FetchCall(
        val sessionId: String,
        val adaptivePricingAllowed: Boolean,
    )
}
