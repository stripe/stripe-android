@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class CheckoutEmbeddedResultCallbackHelperTest {

    @Test
    fun `maps Completed to a checkout Completed result`() = testScenario {
        helper.setResult(EmbeddedPaymentElement.Result.Completed())

        assertThat(results.awaitItem()).isInstanceOf<CheckoutController.Result.Completed>()
    }

    @Test
    fun `maps Canceled to a checkout Canceled result`() = testScenario {
        helper.setResult(EmbeddedPaymentElement.Result.Canceled())

        assertThat(results.awaitItem()).isInstanceOf<CheckoutController.Result.Canceled>()
    }

    @Test
    fun `maps Failed to a checkout Failed result preserving the error`() = testScenario {
        val error = IllegalStateException("boom")

        helper.setResult(EmbeddedPaymentElement.Result.Failed(error))

        val result = results.awaitItem()
        assertThat(result).isInstanceOf<CheckoutController.Result.Failed>()
        assertThat((result as CheckoutController.Result.Failed).error).isEqualTo(error)
    }

    private fun testScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val results = Turbine<CheckoutController.Result>()
        val helper = CheckoutEmbeddedResultCallbackHelper(
            resultCallback = { results.add(it) },
        )
        Scenario(helper = helper, results = results).block()
        results.ensureAllEventsConsumed()
    }

    private class Scenario(
        val helper: CheckoutEmbeddedResultCallbackHelper,
        val results: Turbine<CheckoutController.Result>,
    )
}
