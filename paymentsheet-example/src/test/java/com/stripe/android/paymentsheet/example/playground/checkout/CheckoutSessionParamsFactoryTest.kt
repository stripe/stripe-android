package com.stripe.android.paymentsheet.example.playground.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.example.playground.checkout.settings.AdaptivePricingCountry
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundDefinitions.session
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundSettings
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class CheckoutSessionParamsFactoryTest {
    @Test
    fun `invoice creation is omitted by default`() = runScenario {
        val params = createParams()

        assertThat(params).doesNotContainKey("invoice_creation")
    }

    @Test
    fun `invoice creation includes invoice data when enabled`() = runScenario {
        settings.update(session.invoiceCreation, true)

        val params = createParams()

        val invoiceCreation = params.getValue("invoice_creation").jsonObject
        assertThat(invoiceCreation["enabled"]).isEqualTo(JsonPrimitive(true))
        val invoiceData = invoiceCreation.getValue("invoice_data").jsonObject
        assertThat(invoiceData.string("description")).isEqualTo("Checkout live post-purchase invoice")
        assertThat(invoiceData.getValue("metadata").jsonObject.string("suite"))
            .isEqualTo("checkout_live_billing_invoice")
    }

    @Test
    fun `default parameters contain fixed Elements cart`() = runScenario {
        val params = createParams()

        assertThat(params.string("ui_mode")).isEqualTo("mobile_elements")
        assertThat(params.string("currency")).isEqualTo("usd")
        assertThat(params).doesNotContainKey("allowed_payment_method_types")
        val items = params["items"]!!.jsonArray
        assertThat(items).hasSize(1)
        val lineItems = items.single().jsonObject["one_time_price"]!!.jsonObject["items"]!!.jsonArray
        assertThat(lineItems.map { it.jsonObject.string("quantity") }).containsExactly("2", "1").inOrder()
        assertThat(lineItems.map { it.jsonObject["price_data"]!!.jsonObject.string("unit_amount") })
            .containsExactly("3500", "5000").inOrder()
        assertThat(
            lineItems.map {
                it.jsonObject["price_data"]!!.jsonObject["product_data"]!!.jsonObject.string("name")
            }
        ).containsExactly("Classic T-Shirt", "Zip-Up Hoodie").inOrder()
    }

    @Test
    fun `manual payment methods are sorted and automatic payment methods are omitted`() = runScenario {
        settings.update(session.automaticPaymentMethods, false)
        settings.update(session.allowedPaymentMethodTypes, listOf("klarna", "card"))

        val params = createParams()

        assertThat(params["allowed_payment_method_types"]).isEqualTo(
            JsonArray(listOf(JsonPrimitive("card"), JsonPrimitive("klarna")))
        )
        assertThat(params).doesNotContainKey("payment_method_types")
    }

    @Test
    fun `guest saving creates customer and saved method options`() = runScenario {
        val params = createParams()

        assertThat(params.string("customer_email")).isEqualTo("email@example.com")
        assertThat(params.string("customer_creation")).isEqualTo("always")
        assertThat(params["saved_payment_method_options"]!!.jsonObject.string("payment_method_save"))
            .isEqualTo("enabled")
    }

    @Test
    fun `blank guest email is omitted`() = runScenario {
        settings.update(session.customerEmail, "  ")

        val params = createParams()

        assertThat(params).doesNotContainKey("customer_email")
    }

    @Test
    fun `adaptive pricing email takes precedence`() = runScenario {
        settings.update(session.customerEmail, "ordinary@example.com")
        settings.update(session.adaptivePricingCountry, AdaptivePricingCountry.Japan)

        val params = createParams()

        assertThat(params.string("customer_email")).isEqualTo("test+location_JP@example.com")
    }

    @Test
    fun `customer parameters include tax and address updates`() = runScenario {
        settings.update(session.automaticTax, true)
        settings.update(session.billingAddressCollection, true)
        settings.update(session.shippingAddressCollection, true)

        val params = createParams(customerId = "cus_123")

        assertThat(params.string("customer")).isEqualTo("cus_123")
        assertThat(params["automatic_tax"]!!.jsonObject["enabled"]!!.jsonPrimitive.content).isEqualTo("true")
        assertThat(params.string("billing_address_collection")).isEqualTo("required")
        assertThat(params["shipping_address_collection"]!!.jsonObject["allowed_countries"]!!.jsonArray)
            .containsExactlyElementsIn(listOf("US", "CA", "IE", "GB").map(::JsonPrimitive)).inOrder()
        assertThat(params["customer_update"]!!.jsonObject).isEqualTo(
            JsonObject(mapOf("address" to JsonPrimitive("auto"), "shipping" to JsonPrimitive("auto")))
        )
    }

    @Test
    fun `currency is applied to session and every line item`() = runScenario {
        settings.update(session.currency, Currency.JPY)

        val params = createParams()

        assertThat(params.string("currency")).isEqualTo("jpy")
        val lineItems = params["items"]!!.jsonArray.single().jsonObject["one_time_price"]!!
            .jsonObject["items"]!!.jsonArray
        assertThat(lineItems.map { it.jsonObject["price_data"]!!.jsonObject.string("currency") })
            .containsExactly("jpy", "jpy")
    }

    private fun runScenario(block: Scenario.() -> Unit) {
        Scenario(CheckoutPlaygroundSettings.createInMemory()).block()
    }

    private data class Scenario(val settings: CheckoutPlaygroundSettings) {
        fun createParams(customerId: String? = null): JsonObject {
            return CheckoutSessionParamsFactory.create(settings.snapshot(), customerId)
        }
    }
}

private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content
