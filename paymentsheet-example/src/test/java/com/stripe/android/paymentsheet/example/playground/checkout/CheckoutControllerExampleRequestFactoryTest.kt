package com.stripe.android.paymentsheet.example.playground.checkout

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Test

class CheckoutControllerExampleRequestFactoryTest {
    @Test
    fun `default settings omit address collection fields`() {
        val request = CheckoutControllerExampleRequestFactory.create(settings())

        assertThat(request.endpoint).isEqualTo("checkout_session")
        assertThat(request.body).isEqualTo(
            expectedRequest(
                customer = "guest",
                automaticTax = false,
            )
        )
    }

    @Test
    fun `automatic tax off omits retained address collection fields`() {
        val request = CheckoutControllerExampleRequestFactory.create(
            settings(
                automaticTax = false,
                shippingAddressCollection = false,
                billingAddressCollection = true,
            )
        )

        assertThat(request.body).isEqualTo(
            expectedRequest(
                customer = "guest",
                automaticTax = false,
            )
        )
    }

    @Test
    fun `shipping only settings create expected request`() {
        val request = CheckoutControllerExampleRequestFactory.create(
            settings(
                automaticTax = true,
                shippingAddressCollection = true,
                billingAddressCollection = false,
            )
        )

        assertThat(request.body).isEqualTo(
            expectedRequest(
                customer = "guest",
                automaticTax = true,
                shippingAddressCollection = true,
            )
        )
    }

    @Test
    fun `billing only settings create expected request`() {
        val request = CheckoutControllerExampleRequestFactory.create(
            settings(
                automaticTax = true,
                shippingAddressCollection = false,
                billingAddressCollection = true,
            )
        )

        assertThat(request.body).isEqualTo(
            expectedRequest(
                customer = "guest",
                automaticTax = true,
                shippingAddressCollection = false,
                billingAddressCollection = true,
            )
        )
    }

    @Test
    fun `shipping and billing settings create expected request`() {
        val request = CheckoutControllerExampleRequestFactory.create(
            settings(
                automaticTax = true,
                shippingAddressCollection = true,
                billingAddressCollection = true,
            )
        )

        assertThat(request.body).isEqualTo(
            expectedRequest(
                customer = "guest",
                automaticTax = true,
                shippingAddressCollection = true,
                billingAddressCollection = true,
            )
        )
    }

    @Test
    fun `neither address collection setting creates expected request`() {
        val request = CheckoutControllerExampleRequestFactory.create(
            settings(
                automaticTax = true,
                shippingAddressCollection = false,
                billingAddressCollection = false,
            )
        )

        assertThat(request.body).isEqualTo(
            expectedRequest(
                customer = "guest",
                automaticTax = true,
                shippingAddressCollection = false,
            )
        )
    }

    @Test
    fun `guest settings ignore a stored customer ID`() {
        val request = CheckoutControllerExampleRequestFactory.create(
            settings(
                storedCustomerId = "cus_123",
                customer = CheckoutControllerExampleCustomer.Guest,
            )
        )

        assertThat(request.body).isEqualTo(
            expectedRequest(
                customer = "guest",
                automaticTax = false,
            )
        )
    }

    @Test
    fun `new customer settings create a customer despite a stored ID`() {
        val request = CheckoutControllerExampleRequestFactory.create(
            settings(
                storedCustomerId = "cus_123",
                customer = CheckoutControllerExampleCustomer.New,
            )
        )

        assertThat(request.body).isEqualTo(
            expectedRequest(
                customer = "new",
                automaticTax = false,
                savesPaymentMethod = true,
            )
        )
    }

    @Test
    fun `existing customer settings use the stored ID and enable payment method saving`() {
        val request = CheckoutControllerExampleRequestFactory.create(
            settings(
                storedCustomerId = "cus_123",
                customer = CheckoutControllerExampleCustomer.Existing("cus_123"),
            )
        )

        assertThat(request.body).isEqualTo(
            expectedRequest(
                customer = "cus_123",
                automaticTax = false,
                savesPaymentMethod = true,
            )
        )
    }

    @Test
    fun `existing customer settings fail without a customer ID`() {
        val failure = runCatching {
            CheckoutControllerExampleRequestFactory.create(
                settings(customer = CheckoutControllerExampleCustomer.Existing(""))
            )
        }.exceptionOrNull()

        assertThat(failure).hasMessageThat().isEqualTo("No customer ID available for existing customer")
    }

    private fun settings(
        storedCustomerId: String? = null,
        customer: CheckoutControllerExampleCustomer? = null,
        automaticTax: Boolean = false,
        shippingAddressCollection: Boolean = true,
        billingAddressCollection: Boolean = false,
    ): CheckoutControllerExampleSettings.Snapshot {
        var settings = CheckoutControllerExampleSettings.create(
            persistedValues = null,
            storedCustomerId = storedCustomerId,
        )
        customer?.let { value ->
            settings = settings.withValue(CheckoutControllerExampleSettingsDefinition.Customer, value)
        }
        settings = settings.withValue(
            CheckoutControllerExampleSettingsDefinition.AutomaticTax,
            automaticTax,
        )
        settings = settings.withValue(
            CheckoutControllerExampleSettingsDefinition.ShippingAddressCollection,
            shippingAddressCollection,
        )
        settings = settings.withValue(
            CheckoutControllerExampleSettingsDefinition.BillingAddressCollection,
            billingAddressCollection,
        )
        return settings.snapshot()
    }

    private fun expectedRequest(
        customer: String,
        automaticTax: Boolean,
        shippingAddressCollection: Boolean? = null,
        billingAddressCollection: Boolean = false,
        savesPaymentMethod: Boolean = false,
    ) = buildJsonObject {
        put("mode", "payment")
        put("customer_email", "email@example.com")
        put("currency", "usd")
        put("payment_method_types", commonPaymentMethodTypes)
        put("customer", customer)
        put("automatic_tax", automaticTax)
        shippingAddressCollection?.let { value ->
            put("shipping_address_collection", value)
        }
        if (billingAddressCollection) {
            put("billing_address_collection", true)
        }
        if (savesPaymentMethod) {
            put("checkout_session_payment_method_save", "enabled")
        }
    }

    private companion object {
        val commonPaymentMethodTypes = JsonArray(
            listOf(
                JsonPrimitive("card"),
                JsonPrimitive("us_bank_account"),
                JsonPrimitive("link"),
                JsonPrimitive("cashapp"),
                JsonPrimitive("klarna"),
            )
        )
    }
}
