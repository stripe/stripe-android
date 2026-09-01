package com.stripe.android.paymentsheet.example.playground.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundDefinitions.session
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundSettings
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Test

class CheckoutControllerExampleRequestFactoryTest {
    @Test
    fun `defaults create a Checkout Session request`() = runScenario {
        val request = CheckoutControllerExampleRequestFactory.create(
            settings = settings.snapshot(),
            returningCustomerId = null,
        )

        assertThat(request.endpoint).isEqualTo("checkout_session")
        assertThat(request.body).isEqualTo(
            buildJsonObject {
                put("customer", "guest")
                put("customer_email", "email@example.com")
                put("currency", "usd")
                put("automatic_payment_methods", true)
                put("automatic_tax", false)
                put("shipping_address_collection", false)
                put("billing_address_collection", false)
            }
        )
    }

    @Test
    fun `session settings are included in request`() = runScenario {
        settings.update(session.customer, "new")
        settings.update(session.customerEmail, "another@example.com")
        settings.update(session.currency, Currency.EUR)
        settings.update(session.automaticTax, true)
        settings.update(session.shippingAddressCollection, true)
        settings.update(session.billingAddressCollection, true)

        val request = CheckoutControllerExampleRequestFactory.create(
            settings = settings.snapshot(),
            returningCustomerId = null,
        )

        assertThat(request.body).isEqualTo(
            buildJsonObject {
                put("customer", "new")
                put("customer_email", "another@example.com")
                put("currency", "eur")
                put("automatic_payment_methods", true)
                put("automatic_tax", true)
                put("shipping_address_collection", true)
                put("billing_address_collection", true)
            }
        )
    }

    @Test
    fun `returning customer uses saved customer ID`() = runScenario {
        settings.update(session.customer, "returning")

        val request = CheckoutControllerExampleRequestFactory.create(
            settings = settings.snapshot(),
            returningCustomerId = "cus_123",
        )

        assertThat(request.body["customer"]).isEqualTo(JsonPrimitive("cus_123"))
    }

    @Test
    fun `manual payment methods are included in request`() = runScenario {
        settings.update(session.automaticPaymentMethods, false)
        settings.update(session.paymentMethodTypes, listOf("card", "klarna"))

        val request = CheckoutControllerExampleRequestFactory.create(
            settings = settings.snapshot(),
            returningCustomerId = null,
        )

        assertThat(request.body).containsEntry(
            "payment_method_types",
            JsonArray(listOf(JsonPrimitive("card"), JsonPrimitive("klarna"))),
        )
        assertThat(request.body).doesNotContainKey("automatic_payment_methods")
    }

    private fun runScenario(block: Scenario.() -> Unit) {
        block(Scenario(CheckoutPlaygroundSettings.createInMemory()))
    }

    private data class Scenario(
        val settings: CheckoutPlaygroundSettings,
    )
}
