package com.stripe.android.paymentsheet.example.playground.checkout

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Test

class CheckoutControllerExampleRequestFactoryTest {
    @Test
    fun `no tax scenario creates expected request`() {
        val request = CheckoutControllerExampleRequestFactory.create(
            CheckoutControllerExampleScenario.NoTax
        )

        assertThat(request.endpoint).isEqualTo("checkout_session")
        assertThat(request.body).isEqualTo(
            buildJsonObject {
                put("mode", "payment")
                put("customer", "new")
                put("customer_email", "email@example.com")
                put("currency", "usd")
                put("payment_method_types", commonPaymentMethodTypes)
                put("automatic_tax", false)
                put("shipping_address_collection", false)
                put("billing_address_collection", false)
            }
        )
    }

    @Test
    fun `shipping tax scenario creates expected request`() {
        val request = CheckoutControllerExampleRequestFactory.create(
            CheckoutControllerExampleScenario.ShippingTax
        )

        assertThat(request.endpoint).isEqualTo("checkout_session")
        assertThat(request.body).isEqualTo(
            buildJsonObject {
                put("mode", "payment")
                put("customer", "new")
                put("customer_email", "email@example.com")
                put("currency", "usd")
                put("payment_method_types", commonPaymentMethodTypes)
                put("automatic_tax", true)
                put("shipping_address_collection", true)
                put("billing_address_collection", false)
            }
        )
    }

    @Test
    fun `billing tax scenario creates expected request`() {
        val request = CheckoutControllerExampleRequestFactory.create(
            CheckoutControllerExampleScenario.BillingTax
        )

        assertThat(request.endpoint).isEqualTo("checkout_session")
        assertThat(request.body).isEqualTo(
            buildJsonObject {
                put("mode", "payment")
                put("customer", "guest")
                put("customer_email", "email@example.com")
                put("currency", "usd")
                put("payment_method_types", commonPaymentMethodTypes)
                put("automatic_tax", true)
                put("shipping_address_collection", false)
            }
        )
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
