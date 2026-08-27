package com.stripe.android.paymentsheet.example.playground.checkout

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal enum class CheckoutControllerExampleScenario(
    val displayName: String,
) {
    NoTax("No tax"),
    ShippingTax("Shipping tax"),
    BillingTax("Billing tax w/ auto collection"),
}

internal data class CheckoutControllerExampleRequest(
    val endpoint: String,
    val body: JsonObject,
)

internal object CheckoutControllerExampleRequestFactory {
    fun create(scenario: CheckoutControllerExampleScenario): CheckoutControllerExampleRequest {
        return CheckoutControllerExampleRequest(
            endpoint = "checkout_session",
            body = buildJsonObject {
                put("mode", "payment")
                put(
                    "customer",
                    if (scenario == CheckoutControllerExampleScenario.BillingTax) "guest" else "new"
                )
                put("customer_email", "email@example.com")
                put("currency", "usd")
                put(
                    "payment_method_types",
                    JsonArray(
                        listOf(
                            JsonPrimitive("card"),
                            JsonPrimitive("us_bank_account"),
                            JsonPrimitive("link"),
                            JsonPrimitive("cashapp"),
                            JsonPrimitive("klarna"),
                        )
                    )
                )
                put("automatic_tax", scenario != CheckoutControllerExampleScenario.NoTax)
                put("shipping_address_collection", scenario == CheckoutControllerExampleScenario.ShippingTax)
                if (scenario != CheckoutControllerExampleScenario.BillingTax) {
                    put("billing_address_collection", false)
                }
            },
        )
    }
}
