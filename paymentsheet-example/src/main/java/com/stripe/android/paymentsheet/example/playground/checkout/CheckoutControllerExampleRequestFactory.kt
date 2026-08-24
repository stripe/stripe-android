package com.stripe.android.paymentsheet.example.playground.checkout

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal enum class CheckoutControllerExampleScenario(
    val displayName: String,
) {
    NoTax("No tax"),
    ShippingTax("Shipping tax"),
    BillingTax("Billing tax"),
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
                put("customer", "new")
                put("customer_email", "email@example.com")
                put("currency", "usd")
                put("automatic_tax", scenario != CheckoutControllerExampleScenario.NoTax)
                put("shipping_address_collection", scenario == CheckoutControllerExampleScenario.ShippingTax)
                put(
                    "billing_address_collection",
                    if (scenario == CheckoutControllerExampleScenario.BillingTax) "required" else "auto",
                )
            },
        )
    }
}
