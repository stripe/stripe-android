package com.stripe.android.paymentsheet.example.playground.checkout

import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundDefinitions.session
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundSettings
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
    fun create(settings: CheckoutPlaygroundSettings.Snapshot): CheckoutControllerExampleRequest {
        return CheckoutControllerExampleRequest(
            endpoint = "checkout_session",
            body = buildJsonObject {
                val customer = settings[session.customer]
                val customerId = if (session.customerId.isApplicable(settings)) {
                    settings[session.customerId]
                } else {
                    null
                }
                put("customer", customerId ?: customer)
                if (session.paymentMethodSave.isApplicable(settings)) {
                    put(
                        "checkout_session_payment_method_save",
                        if (settings[session.paymentMethodSave]) "enabled" else "disabled",
                    )
                }
                put("customer_email", settings[session.customerEmail])
                put("currency", settings[session.currency].value)
                if (session.paymentMethodTypes.isApplicable(settings)) {
                    put(
                        "payment_method_types",
                        JsonArray(
                            settings[session.paymentMethodTypes].map(::JsonPrimitive)
                        )
                    )
                } else {
                    put("automatic_payment_methods", true)
                }
                put("automatic_tax", settings[session.automaticTax])
                put("shipping_address_collection", settings[session.shippingAddressCollection])
                put("billing_address_collection", settings[session.billingAddressCollection])
            },
        )
    }
}
