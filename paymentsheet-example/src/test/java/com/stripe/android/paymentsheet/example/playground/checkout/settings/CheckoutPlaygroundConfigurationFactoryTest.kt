@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout.settings

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CheckoutPlaygroundConfigurationFactoryTest {
    @Test
    fun `default tree builds CheckoutElementsController configuration`() = runScenario {
        val configuration = settings.snapshot().checkoutElementsControllerConfiguration()

        assertThat(configuration).isNotNull()
    }

    @Test
    fun `nested overrides build CheckoutElementsController configuration`() = runScenario {
        settings.update(CheckoutPlaygroundDefinitions.Controller.defaults.billing.enabled, true)
        settings.update(CheckoutPlaygroundDefinitions.Controller.defaults.billing.address.enabled, true)
        settings.update(
            CheckoutPlaygroundDefinitions.Controller.payment.appearance.colorsLight.primary,
            Color(0xFF112233),
        )
        settings.update(CheckoutPlaygroundDefinitions.Controller.currencySelector.appearance.cornerRadiusDp, 8f)

        val configuration = settings.snapshot().checkoutElementsControllerConfiguration()

        assertThat(configuration).isNotNull()
    }

    @Test
    fun `flat with checkmark row style builds CheckoutElementsController configuration`() = runScenario {
        settings.update(rowStyle.flatWithCheckmark.checkmarkInsetDp, 4f)

        assertRowStyleBuilds(CheckoutEmbeddedRowStyle.FlatWithCheckmark)
    }

    @Test
    fun `floating button row style builds CheckoutElementsController configuration`() = runScenario {
        settings.update(rowStyle.floatingButton.spacingDp, 4f)

        assertRowStyleBuilds(CheckoutEmbeddedRowStyle.FloatingButton)
    }

    @Test
    fun `flat with disclosure row style builds CheckoutElementsController configuration`() = runScenario {
        settings.update(rowStyle.flatWithDisclosure.disclosureIconRes, CheckoutDisclosureIcon.Add)
        settings.update(rowStyle.flatWithDisclosure.colorsLight.disclosureColor, Color(0xFF112233))

        assertRowStyleBuilds(CheckoutEmbeddedRowStyle.FlatWithDisclosure)
    }

    @Test
    fun `appearance additions build CheckoutElementsController configuration`() = runScenario {
        val appearance = CheckoutPlaygroundDefinitions.Controller.payment.appearance
        settings.update(appearance.shapes.bottomSheetCornerRadiusDp, 12f)
        settings.update(appearance.typography.sizeScaleFactor, 1.5f)
        settings.update(appearance.typography.custom.h1.fontWeight, 700)
        settings.update(appearance.embeddedAppearance.paymentMethodIconMargins.startDp, 8f)
        settings.update(appearance.embeddedAppearance.titleFont.letterSpacingSp, -0.5f)
        settings.update(
            CheckoutPlaygroundDefinitions.Controller.shippingAddress.appearance.shapes.cornerRadiusDp,
            0f,
        )
        settings.update(CheckoutPlaygroundDefinitions.Controller.express.shouldSetConfiguration, true)
        settings.update(CheckoutPlaygroundDefinitions.Controller.express.appearance.cornerRadiusDp, 20f)

        val configuration = settings.snapshot().checkoutElementsControllerConfiguration()

        assertThat(configuration).isNotNull()
    }

    private fun Scenario.assertRowStyleBuilds(type: CheckoutEmbeddedRowStyle) {
        settings.update(rowStyle.type, type)

        val configuration = settings.snapshot().checkoutElementsControllerConfiguration()

        assertThat(configuration).isNotNull()
    }

    private val rowStyle = CheckoutPlaygroundDefinitions.Controller.payment.appearance.embeddedAppearance.rowStyle

    private fun runScenario(block: Scenario.() -> Unit) {
        block(Scenario(CheckoutPlaygroundSettings.createInMemory()))
    }

    private data class Scenario(
        val settings: CheckoutPlaygroundSettings,
    )
}
