package com.stripe.android.paymentelement.embedded.content

import android.os.Bundle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedResultCallbackHelper
import com.stripe.android.paymentelement.embedded.EmbeddedResultCallbackHelper
import com.stripe.android.paymentelement.embedded.linkinline.FakeEmbeddedLinkInlineAuth
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
        // The inline Link verification card never comes back after the intent is confirmed.
        linkInlineAuth.onIntentConfirmedCalls.awaitItem()
    }

    private fun testScenario(
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val linkInlineAuth = FakeEmbeddedLinkInlineAuth()
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
            stateHelper = stateHelper,
            linkInlineAuth = linkInlineAuth,
        )
        Scenario(
            stateHelper = stateHelper,
            resultCallbackTurbine = resultCallbackTurbine,
            helper = helper,
            linkInlineAuth = linkInlineAuth,
        ).block()
        resultCallbackTurbine.ensureAllEventsConsumed()
        stateHelper.validate()
        linkInlineAuth.ensureAllEventsConsumed()
    }

    private class Scenario(
        val stateHelper: FakeEmbeddedStateHelper,
        val resultCallbackTurbine: ReceiveTurbine<EmbeddedPaymentElement.Result>,
        val helper: EmbeddedResultCallbackHelper,
        val linkInlineAuth: FakeEmbeddedLinkInlineAuth,
    )
}
