package com.stripe.android.paymentsheet.example.playground.checkout.settings

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CheckoutPlaygroundSettingsTest {
    @Test
    fun `backend URL defaults to configured playground backend`() {
        val settings = CheckoutPlaygroundSettings.createInMemory(
            json = null,
            defaultBackendUrl = "https://default.example/",
        )

        assertThat(settings[CheckoutPlaygroundDefinitions.session.backendUrl])
            .isEqualTo("https://default.example/")
    }

    @Test
    fun `reset restores configured playground backend URL`() {
        val definition = CheckoutPlaygroundDefinitions.session.backendUrl
        val settings = CheckoutPlaygroundSettings.createInMemory(
            json = null,
            defaultBackendUrl = "https://default.example/",
        )
        settings.update(definition, "https://custom.example/")

        settings.reset()

        assertThat(settings[definition]).isEqualTo("https://default.example/")
    }

    @Test
    fun `nested setting survives JSON round trip`() = runScenario {
        settings.update(CheckoutPlaygroundDefinitions.Controller.currencySelector.appearance.scale, 1.25f)

        val restored = CheckoutPlaygroundSettings.createInMemory(settings.asJsonString())

        assertThat(restored[CheckoutPlaygroundDefinitions.Controller.currencySelector.appearance.scale])
            .isEqualTo(1.25f)
    }

    @Test
    fun `payment method saving survives JSON round trip`() = runScenario {
        val definition = CheckoutPlaygroundDefinitions.session.paymentMethodSave
        settings.update(definition, false)

        val restored = CheckoutPlaygroundSettings.createInMemory(settings.asJsonString())

        assertThat(restored[definition]).isFalse()
    }

    @Test
    fun `customer ID survives JSON round trip`() = runScenario {
        val definition = CheckoutPlaygroundDefinitions.session.customerId
        settings.update(definition, "cus_custom")

        val restored = CheckoutPlaygroundSettings.createInMemory(settings.asJsonString())

        assertThat(restored[definition]).isEqualTo("cus_custom")
    }

    @Test
    fun `unknown and invalid persisted values fall back to defaults`() = runScenario(
        json = """{"unknown":"value","currency.appearance.scale":"not-a-number"}"""
    ) {
        assertThat(settings[CheckoutPlaygroundDefinitions.Controller.currencySelector.appearance.scale])
            .isEqualTo(1f)
    }

    @Test
    fun `reset restores nested defaults`() = runScenario {
        val definition = CheckoutPlaygroundDefinitions.Controller.payment.embeddedMandate
        settings.update(definition, false)

        settings.reset()

        assertThat(settings[definition]).isTrue()
    }

    @Test
    fun `terms display defaults to automatic`() = runScenario {
        val definitions = CheckoutPlaygroundDefinitions.Controller.payment.terms.values.values

        assertThat(definitions.map { settings[it] }.distinct())
            .containsExactly(CheckoutTermsDisplay.Automatic)
    }

    @Test
    fun `payment colors default to effective SDK colors`() = runScenario {
        val lightColors = CheckoutPlaygroundDefinitions.Controller.payment.appearance.lightColors
        val darkColors = CheckoutPlaygroundDefinitions.Controller.payment.appearance.darkColors

        assertThat(lightColors.configuration.values().map(settings::serializedValue))
            .doesNotContain("")
        assertThat(darkColors.configuration.values().map(settings::serializedValue))
            .doesNotContain("")
    }

    @Test
    fun `invalid color prevents snapshot`() = runScenario {
        val definition = CheckoutPlaygroundDefinitions.Controller.currencySelector.appearance.background
        settings.updateSerialized(definition, "blue")

        assertThat(settings.validationErrors())
            .containsKey(definition)
    }

    @Test
    fun `typed color survives JSON round trip`() = runScenario {
        val definition = CheckoutPlaygroundDefinitions.Controller.currencySelector.appearance.background
        settings.update(definition, Color(0xFF112233))

        val restored = CheckoutPlaygroundSettings.createInMemory(settings.asJsonString())

        assertThat(restored[definition]).isEqualTo(Color(0xFF112233))
    }

    @Test
    fun `import replaces current settings`() = runScenario {
        val currency = CheckoutPlaygroundDefinitions.session.currency
        val paymentMethodSave = CheckoutPlaygroundDefinitions.session.paymentMethodSave
        settings.updateSerialized(currency, "eur")
        settings.update(paymentMethodSave, false)
        val exportedJson = settings.asJsonString()
        settings.updateSerialized(currency, "usd")
        settings.update(paymentMethodSave, true)

        val result = settings.importJson(exportedJson)

        assertThat(result.isSuccess).isTrue()
        assertThat(settings.serializedValue(currency)).isEqualTo("eur")
        assertThat(settings[paymentMethodSave]).isFalse()
    }

    @Test
    fun `malformed import does not change current settings`() = runScenario {
        val definition = CheckoutPlaygroundDefinitions.session.currency
        settings.updateSerialized(definition, "eur")

        val result = settings.importJson("not json")

        assertThat(result.isFailure).isTrue()
        assertThat(settings.serializedValue(definition)).isEqualTo("eur")
    }

    @Test
    fun `import ignores unknown setting`() = runScenario {
        val definition = CheckoutPlaygroundDefinitions.session.currency
        settings.updateSerialized(definition, "usd")

        val result = settings.importJson("""{"unknown":"value","session.currency":"eur"}""")

        assertThat(result.isSuccess).isTrue()
        assertThat(settings.serializedValue(definition)).isEqualTo("eur")
    }

    @Test
    fun `import with invalid setting does not change current settings`() = runScenario {
        val definition = CheckoutPlaygroundDefinitions.Controller.currencySelector.appearance.scale
        settings.update(definition, 1.25f)

        val result = settings.importJson("""{"currency.appearance.scale":"not-a-number"}""")

        assertThat(result.isFailure).isTrue()
        assertThat(settings[definition]).isEqualTo(1.25f)
    }

    @Test
    fun `new customer is saved as returning customer`() = runScenario {
        settings.update(CheckoutPlaygroundDefinitions.session.customer, "new")

        settings.saveReturningCustomer("cus_123")

        assertThat(settings.returningCustomerId).isEqualTo("cus_123")
        assertThat(settings[CheckoutPlaygroundDefinitions.session.customer]).isEqualTo("returning")
        assertThat(settings[CheckoutPlaygroundDefinitions.session.customerId]).isEqualTo("cus_123")
    }

    @Test
    fun `reset preserves returning customer ID as default`() = runScenario {
        settings.saveReturningCustomer("cus_123")
        settings.update(CheckoutPlaygroundDefinitions.session.customerId, "cus_custom")

        settings.reset()

        assertThat(settings.returningCustomerId).isEqualTo("cus_123")
        assertThat(settings[CheckoutPlaygroundDefinitions.session.customerId]).isEqualTo("cus_123")
        assertThat(settings[CheckoutPlaygroundDefinitions.session.customer]).isEqualTo("guest")
    }

    private fun runScenario(
        json: String? = null,
        block: Scenario.() -> Unit,
    ) {
        block(Scenario(CheckoutPlaygroundSettings.createInMemory(json)))
    }

    private data class Scenario(
        val settings: CheckoutPlaygroundSettings,
    )
}
