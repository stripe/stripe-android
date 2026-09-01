package com.stripe.android.paymentsheet.example.playground.checkout

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal data class CheckoutControllerExampleRequest(
    val endpoint: String,
    val body: JsonObject,
)

internal object CheckoutControllerExampleRequestFactory {
    fun create(settings: CheckoutControllerExampleSettings.Snapshot): CheckoutControllerExampleRequest {
        return CheckoutControllerExampleRequest(
            endpoint = "checkout_session",
            body = buildJsonObject {
                put("mode", "payment")
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
                settings.applyTo(this)
            },
        )
    }
}
