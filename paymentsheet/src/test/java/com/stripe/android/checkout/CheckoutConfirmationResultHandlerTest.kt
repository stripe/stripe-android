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
import com.stripe.android.paymentelement.confirmation.MutableConfirmationMetadata
import com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionResponseKey
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutConfirmationResultHandlerTest {

    @Test
    fun `register handles current complete response before invoking callback`() {
        val response = response(status = CheckoutSessionResponse.Status.COMPLETE)

        runScenario(initialResult = succeeded(response)) {
            val callbackCall = callbackCalls.awaitItem()
            assertThat(callbackCall.result).isInstanceOf<CheckoutController.Result.Completed>()
            assertThat(callbackCall.succeededCallCount).isEqualTo(1)
            assertThat(callbackCall.succeededResponse).isEqualTo(response)
        }
    }

    @Test
    fun `succeeded with open response passes it to success handler and invokes Completed`() = runScenario {
        val response = response(status = CheckoutSessionResponse.Status.OPEN)

        emit(succeeded(response))

        val callbackCall = callbackCalls.awaitItem()
        assertThat(callbackCall.result).isInstanceOf<CheckoutController.Result.Completed>()
        assertThat(callbackCall.succeededResponse).isEqualTo(response)
    }

    @Test
    fun `succeeded with unknown response passes it to success handler and invokes Completed`() = runScenario {
        val response = response(status = CheckoutSessionResponse.Status.UNKNOWN)

        emit(succeeded(response))

        val callbackCall = callbackCalls.awaitItem()
        assertThat(callbackCall.result).isInstanceOf<CheckoutController.Result.Completed>()
        assertThat(callbackCall.succeededResponse).isEqualTo(response)
    }

    @Test
    fun `succeeded with expired response passes it to success handler and invokes Completed`() = runScenario {
        val response = response(status = CheckoutSessionResponse.Status.EXPIRED)

        emit(succeeded(response))

        val callbackCall = callbackCalls.awaitItem()
        assertThat(callbackCall.result).isInstanceOf<CheckoutController.Result.Completed>()
        assertThat(callbackCall.succeededResponse).isEqualTo(response)
    }

    @Test
    fun `succeeded result without response invokes success handler before Completed`() = runScenario {
        emit(ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED))

        val callbackCall = callbackCalls.awaitItem()
        assertThat(callbackCall.result).isInstanceOf<CheckoutController.Result.Completed>()
        assertThat(callbackCall.succeededCallCount).isEqualTo(1)
        assertThat(callbackCall.succeededResponse).isNull()
    }

    @Test
    fun `each confirmation completion delivers its own callback`() = runScenario {
        val response = response(status = CheckoutSessionResponse.Status.COMPLETE)

        emit(succeeded(response))
        assertThat(callbackCalls.awaitItem().result).isInstanceOf<CheckoutController.Result.Completed>()

        // A second confirmation returns to Confirming before completing again. The interleaved
        // Confirming breaks the state flow's de-duplication, so an identical result still delivers.
        emitConfirming()
        emit(succeeded(response))

        assertThat(callbackCalls.awaitItem().result).isInstanceOf<CheckoutController.Result.Completed>()
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

        val result = callbackCalls.awaitItem().result
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

        assertThat(callbackCalls.awaitItem().result).isInstanceOf<CheckoutController.Result.Canceled>()
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

        assertThat(callbackCalls.awaitItem().result).isInstanceOf<CheckoutController.Result.Canceled>()
    }

    private fun runScenario(
        initialResult: ConfirmationHandler.Result? = null,
        hasReloadedFromProcessDeath: Boolean = false,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val callbackCalls = Turbine<CallbackCall>()
        var succeededCallCount = 0
        var succeededResponse: CheckoutSessionResponse? = null
        val initialConfirmationState: ConfirmationHandler.State =
            initialResult?.let(ConfirmationHandler.State::Complete)
                ?: ConfirmationHandler.State.Idle
        val confirmationHandler = FakeConfirmationHandler(
            hasReloadedFromProcessDeath = hasReloadedFromProcessDeath,
            state = MutableStateFlow(initialConfirmationState),
        )
        val handler = CheckoutConfirmationResultHandler(
            confirmationHandler = confirmationHandler,
            resultCallback = CheckoutController.ResultCallback { result ->
                callbackCalls.add(
                    CallbackCall(
                        result = result,
                        succeededCallCount = succeededCallCount,
                        succeededResponse = succeededResponse,
                    )
                )
            },
            viewModelScope = backgroundScope,
        )
        handler.register { response ->
            succeededCallCount += 1
            succeededResponse = response
        }
        testScheduler.runCurrent()

        Scenario(
            confirmationHandler = confirmationHandler,
            callbackCalls = callbackCalls,
            testScheduler = testScheduler,
        ).block()

        confirmationHandler.validate()
        callbackCalls.ensureAllEventsConsumed()
    }

    private fun response(
        status: CheckoutSessionResponse.Status,
    ): CheckoutSessionResponse {
        return CheckoutSessionResponseFactory.create(
            id = "cs_confirmed",
            status = status,
        )
    }

    private fun succeeded(
        response: CheckoutSessionResponse,
    ): ConfirmationHandler.Result.Succeeded {
        return ConfirmationHandler.Result.Succeeded(
            intent = PaymentIntentFixtures.PI_SUCCEEDED,
            metadata = MutableConfirmationMetadata().apply {
                set(CheckoutSessionResponseKey, response)
            },
        )
    }

    private class Scenario(
        val confirmationHandler: FakeConfirmationHandler,
        val callbackCalls: Turbine<CallbackCall>,
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

    private data class CallbackCall(
        val result: CheckoutController.Result,
        val succeededCallCount: Int,
        val succeededResponse: CheckoutSessionResponse?,
    )
}
