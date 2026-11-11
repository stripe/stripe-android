package com.stripe.android.checkout

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val CURRENCY = "usd"
private const val CUSTOMER_EMAIL = "checkout-e2e@example.com"

/** Builds a card-only Checkout Session request with a single line item for [CheckoutPlaygroundBackend]. */
internal fun checkoutSessionParams(
    unitAmount: Long = 1000L,
    customerId: String? = null,
    automaticTax: Boolean = false,
    paymentMethodSave: Boolean? = null,
): JsonObject = buildJsonObject {
    put("ui_mode", "mobile_elements")
    put("currency", CURRENCY)
    put("allowed_payment_method_types", JsonArray(listOf(JsonPrimitive("card"))))
    putCart(unitAmount, automaticTax)
    putCustomer(customerId, automaticTax)
    if (automaticTax) {
        put("automatic_tax", buildJsonObject { put("enabled", true) })
        put("billing_address_collection", "required")
    }
    if (paymentMethodSave != null) putSavedPaymentMethodOptions(paymentMethodSave)
}

private fun JsonObjectBuilder.putCart(unitAmount: Long, automaticTax: Boolean) {
    val lineItem = buildJsonObject {
        put("quantity", 1)
        put(
            "price_data",
            buildJsonObject {
                put("currency", CURRENCY)
                put("unit_amount", unitAmount)
                if (automaticTax) put("tax_behavior", "exclusive")
                put(
                    "product_data",
                    buildJsonObject {
                        put("name", "Checkout E2E")
                        if (automaticTax) put("tax_code", "txcd_99999999")
                    },
                )
            },
        )
    }
    put(
        "items",
        buildJsonArray {
            add(
                buildJsonObject {
                    put("type", "one_time_price")
                    put("one_time_price", buildJsonObject { put("items", JsonArray(listOf(lineItem))) })
                },
            )
        },
    )
}

private fun JsonObjectBuilder.putCustomer(customerId: String?, automaticTax: Boolean) {
    if (customerId == null) {
        put("customer_email", CUSTOMER_EMAIL)
        return
    }

    put("customer", customerId)
    if (automaticTax) put("customer_update", buildJsonObject { put("address", "auto") })
}

private fun JsonObjectBuilder.putSavedPaymentMethodOptions(paymentMethodSave: Boolean) {
    put(
        "saved_payment_method_options",
        buildJsonObject {
            put("payment_method_save", if (paymentMethodSave) "enabled" else "disabled")
            put("payment_method_remove", "disabled")
        },
    )
}
