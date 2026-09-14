@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout.settings

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CheckoutPlaygroundConfigurationFactoryTest {
    @Test
    fun `default tree builds CheckoutController configuration`() = runScenario {
        val configuration = settings.snapshot().checkoutControllerConfiguration()

        assertThat(configuration).isNotNull()
    }

    @Test
    fun `nested overrides build CheckoutController configuration`() = runScenario {
        settings.update(CheckoutPlaygroundDefinitions.Controller.defaults.billing.enabled, true)
        settings.update(CheckoutPlaygroundDefinitions.Controller.defaults.billing.address.enabled, true)
        settings.update(
            CheckoutPlaygroundDefinitions.Controller.payment.appearance.lightColors.primary,
            Color(0xFF112233),
        )
        settings.update(CheckoutPlaygroundDefinitions.Controller.currencySelector.appearance.cornerRadius, 8f)

        val configuration = settings.snapshot().checkoutControllerConfiguration()

        assertThat(configuration).isNotNull()
    }

    private fun runScenario(block: Scenario.() -> Unit) {
        block(Scenario(CheckoutPlaygroundSettings.createInMemory()))
    }

    private data class Scenario(
        val settings: CheckoutPlaygroundSettings,
    )
}
