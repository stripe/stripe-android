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
    fun `guest creates a Checkout Session request without payment method saving`() = runScenario {
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
    fun `new customer enables payment method saving`() = runScenario {
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
                put("checkout_session_payment_method_save", "enabled")
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
    fun `new customer can disable payment method saving`() = runScenario {
        settings.update(session.customer, "new")
        settings.update(session.paymentMethodSave, false)

        val request = CheckoutControllerExampleRequestFactory.create(
            settings = settings.snapshot(),
            returningCustomerId = null,
        )

        assertThat(request.body).isEqualTo(
            buildJsonObject {
                put("customer", "new")
                put("checkout_session_payment_method_save", "disabled")
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
    fun `returning customer with saved ID enables payment method saving`() = runScenario {
        settings.update(session.customer, "returning")

        val request = CheckoutControllerExampleRequestFactory.create(
            settings = settings.snapshot(),
            returningCustomerId = "cus_123",
        )

        assertThat(request.body).isEqualTo(
            buildJsonObject {
                put("customer", "cus_123")
                put("checkout_session_payment_method_save", "enabled")
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
    fun `returning customer without saved ID uses fixture and enables payment method saving`() = runScenario {
        settings.update(session.customer, "returning")

        val request = CheckoutControllerExampleRequestFactory.create(
            settings = settings.snapshot(),
            returningCustomerId = null,
        )

        assertThat(request.body).isEqualTo(
            buildJsonObject {
                put("customer", "returning")
                put("checkout_session_payment_method_save", "enabled")
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
