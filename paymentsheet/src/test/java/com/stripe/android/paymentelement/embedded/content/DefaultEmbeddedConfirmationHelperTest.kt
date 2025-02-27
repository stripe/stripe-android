package com.stripe.android.paymentelement.embedded.content

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.mockito.Mockito.mock
import kotlin.test.Test

@ExperimentalEmbeddedPaymentElementApi
internal class DefaultEmbeddedConfirmationHelperTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `if confirm did start, should not call result callback with failure`() = testScenario(
        confirmShouldStart = true,
    ) {
        confirmationHelper.confirm()

        assertThat(confirmationMediator.confirmTurbine.awaitItem()).isNotNull()

        resultCallbackTurbine.expectNoEvents()
    }

    @Test
    fun `if confirm did not start, should call result callback with failure`() = testScenario(
        confirmShouldStart = false,
    ) {
        confirmationHelper.confirm()

        assertThat(confirmationMediator.confirmTurbine.awaitItem()).isNotNull()

        val result = resultCallbackTurbine.awaitItem()

        assertThat(result).isInstanceOf<EmbeddedPaymentElement.Result.Failed>()

        val failedResult = result as EmbeddedPaymentElement.Result.Failed

        assertThat(failedResult.error).isInstanceOf<IllegalStateException>()
        assertThat(failedResult.error.message).isEqualTo("Not in a state that's confirmable.")
    }

    @Test
    fun `on mediator result, should emit through result callback`() = testScenario(
        result = flowOf(EmbeddedPaymentElement.Result.Completed())
    ) {
        assertThat(resultCallbackTurbine.awaitItem()).isInstanceOf<EmbeddedPaymentElement.Result.Completed>()
    }

    private fun testScenario(
        result: Flow<EmbeddedPaymentElement.Result> = flowOf(),
        confirmShouldStart: Boolean = true,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val resultCallbackTurbine = Turbine<EmbeddedPaymentElement.Result>()
        val confirmationMediator = FakeEmbeddedConfirmationMediator(
            result = result,
            confirmShouldStart = confirmShouldStart,
        )

        val activityResultCaller = mock<ActivityResultCaller>()
        val lifecycleOwner = TestLifecycleOwner(coroutineDispatcher = Dispatchers.Unconfined)
        val confirmationHelper = DefaultEmbeddedConfirmationHelper(
            confirmationMediator = confirmationMediator,
            resultCallback = {
                resultCallbackTurbine.add(it)
            },
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
        )

        val registerCall = confirmationMediator.registerTurbine.awaitItem()

        assertThat(registerCall.activityResultCaller).isEqualTo(activityResultCaller)
        assertThat(registerCall.lifecycleOwner).isEqualTo(lifecycleOwner)

        Scenario(
            confirmationHelper = confirmationHelper,
            confirmationMediator = confirmationMediator,
            resultCallbackTurbine = resultCallbackTurbine,
        ).block()

        confirmationMediator.confirmTurbine.ensureAllEventsConsumed()
        confirmationMediator.registerTurbine.ensureAllEventsConsumed()
        resultCallbackTurbine.ensureAllEventsConsumed()
    }

    private class Scenario(
        val confirmationHelper: EmbeddedConfirmationHelper,
        val confirmationMediator: FakeEmbeddedConfirmationMediator,
        val resultCallbackTurbine: ReceiveTurbine<EmbeddedPaymentElement.Result>,
    )

    private class FakeEmbeddedConfirmationMediator(
        override val result: Flow<EmbeddedPaymentElement.Result>,
        private val confirmShouldStart: Boolean,
    ) : EmbeddedConfirmationMediator {
        val registerTurbine: Turbine<RegisterCall> = Turbine()
        val confirmTurbine: Turbine<Unit> = Turbine()

        override fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner) {
            registerTurbine.add(RegisterCall(activityResultCaller, lifecycleOwner))
        }

        override fun confirm(): Boolean {
            confirmTurbine.add(Unit)

            return confirmShouldStart
        }

        data class RegisterCall(
            val activityResultCaller: ActivityResultCaller,
            val lifecycleOwner: LifecycleOwner,
        )
    }
}
