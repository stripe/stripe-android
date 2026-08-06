package com.stripe.android.checkout

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutConfirmationResultHandlerTest {

    @Test
    fun `register handles current complete result`() {
        runScenario(initialResult = succeeded()) {
            assertThat(callbackCalls.awaitItem()).isInstanceOf<CheckoutController.Result.Completed>()
        }
    }

    @Test
    fun `succeeded result invokes Completed`() = runScenario {
        emit(succeeded())

        assertThat(callbackCalls.awaitItem()).isInstanceOf<CheckoutController.Result.Completed>()
    }

    @Test
    fun `each confirmation completion delivers its own callback`() = runScenario {
        emit(succeeded())
        assertThat(callbackCalls.awaitItem()).isInstanceOf<CheckoutController.Result.Completed>()

        // A second confirmation returns to Confirming before completing again. The interleaved
        // Confirming breaks the state flow's de-duplication, so an identical result still delivers.
        emitConfirming()
        emit(succeeded())

        assertThat(callbackCalls.awaitItem()).isInstanceOf<CheckoutController.Result.Completed>()
    }

    @Test
    fun `failed result invokes Failed with the confirmation cause`() = runScenario {
        val cause = IllegalStateException("Failed")

        emit(
            ConfirmationHandler.Result.Failed(
                cause = cause,
                message = "Failed".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        )

        val result = callbackCalls.awaitItem()
        assertThat(result).isInstanceOf<CheckoutController.Result.Failed>()
        assertThat((result as CheckoutController.Result.Failed).error).isEqualTo(cause)
    }

    @Test
    fun `canceled with InformCancellation invokes Canceled`() = runScenario {
        emit(
            ConfirmationHandler.Result.Canceled(
                action = ConfirmationHandler.Result.Canceled.Action.InformCancellation,
            )
        )

        assertThat(callbackCalls.awaitItem()).isInstanceOf<CheckoutController.Result.Canceled>()
    }

    @Test
    fun `canceled with ModifyPaymentDetails does not invoke callback`() = runScenario {
        emit(
            ConfirmationHandler.Result.Canceled(
                action = ConfirmationHandler.Result.Canceled.Action.ModifyPaymentDetails,
            )
        )

        callbackCalls.expectNoEvents()
    }

    @Test
    fun `canceled with None does not invoke callback`() = runScenario {
        emit(
            ConfirmationHandler.Result.Canceled(
                action = ConfirmationHandler.Result.Canceled.Action.None,
            )
        )

        callbackCalls.expectNoEvents()
    }

    @Test
    fun `canceled with None after process death invokes Canceled`() = runScenario(
        hasReloadedFromProcessDeath = true,
    ) {
        emit(
            ConfirmationHandler.Result.Canceled(
                action = ConfirmationHandler.Result.Canceled.Action.None,
            )
        )

        assertThat(callbackCalls.awaitItem()).isInstanceOf<CheckoutController.Result.Canceled>()
    }

    private fun runScenario(
        initialResult: ConfirmationHandler.Result? = null,
        hasReloadedFromProcessDeath: Boolean = false,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val callbackCalls = Turbine<CheckoutController.Result>()
        val initialConfirmationState: ConfirmationHandler.State =
            initialResult?.let(ConfirmationHandler.State::Complete)
                ?: ConfirmationHandler.State.Idle
        val confirmationHandler = FakeConfirmationHandler(
            hasReloadedFromProcessDeath = hasReloadedFromProcessDeath,
            state = MutableStateFlow(initialConfirmationState),
        )
        val handler = CheckoutConfirmationResultHandler(
            confirmationHandler = confirmationHandler,
            resultCallback = CheckoutController.ResultCallback(callbackCalls::add),
            viewModelScope = backgroundScope,
        )
        handler.register()
        testScheduler.runCurrent()

        Scenario(
            confirmationHandler = confirmationHandler,
            callbackCalls = callbackCalls,
            testScheduler = testScheduler,
        ).block()

        confirmationHandler.validate()
        callbackCalls.ensureAllEventsConsumed()
    }

    private fun succeeded(): ConfirmationHandler.Result.Succeeded {
        return ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
    }

    private class Scenario(
        val confirmationHandler: FakeConfirmationHandler,
        val callbackCalls: Turbine<CheckoutController.Result>,
        val testScheduler: TestCoroutineScheduler,
    ) {
        fun emit(result: ConfirmationHandler.Result) {
            confirmationHandler.state.value = ConfirmationHandler.State.Complete(result)
            testScheduler.runCurrent()
        }

        fun emitConfirming() {
            confirmationHandler.state.value =
                ConfirmationHandler.State.Confirming(FakeConfirmationOption())
            testScheduler.runCurrent()
        }
    }
}
