package com.stripe.android.paymentelement.embedded.content

import android.os.Bundle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.payment.EmbeddedPaymentElement
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedResultCallbackHelper
import com.stripe.android.paymentelement.embedded.EmbeddedResultCallbackHelper
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class DefaultEmbeddedResultCallbackHelperTest {

    @Test
    fun `invokes callback on setResult and does not clear state if failed`() = testScenario {
        helper.setResult(EmbeddedPaymentElement.Result.Failed(Throwable("oops")))
        val callbackResult = resultCallbackTurbine.awaitItem()
        assertThat(stateHelper.stateTurbine.awaitItem()).isNotNull()
        assertThat(callbackResult).isInstanceOf<EmbeddedPaymentElement.Result.Failed>()
        stateHelper.stateTurbine.expectNoEvents()
    }

    @Test
    fun `invokes callback on setResult and clears state if completed`() = testScenario {
        helper.setResult(EmbeddedPaymentElement.Result.Completed())
        val callbackResult = resultCallbackTurbine.awaitItem()
        assertThat(stateHelper.stateTurbine.awaitItem()).isNotNull()
        assertThat(callbackResult).isInstanceOf<EmbeddedPaymentElement.Result.Completed>()
        assertThat(stateHelper.stateTurbine.awaitItem()).isNull()
    }

    private fun testScenario(
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val stateHelper = FakeEmbeddedStateHelper()
        stateHelper.state = EmbeddedPaymentElement.State(
            confirmationState = EmbeddedConfirmationStateFixtures.defaultState(),
            customer = null,
            previousNewSelections = Bundle(),
        )
        val resultCallbackTurbine = Turbine<EmbeddedPaymentElement.Result>()
        val helper = DefaultEmbeddedResultCallbackHelper(
            resultCallback = {
                resultCallbackTurbine.add(it)
            },
            stateHelper = stateHelper
        )
        Scenario(
            stateHelper = stateHelper,
            resultCallbackTurbine = resultCallbackTurbine,
            helper = helper
        ).block()
        resultCallbackTurbine.ensureAllEventsConsumed()
        stateHelper.validate()
    }

    private class Scenario(
        val stateHelper: FakeEmbeddedStateHelper,
        val resultCallbackTurbine: ReceiveTurbine<EmbeddedPaymentElement.Result>,
        val helper: EmbeddedResultCallbackHelper
    )
}
