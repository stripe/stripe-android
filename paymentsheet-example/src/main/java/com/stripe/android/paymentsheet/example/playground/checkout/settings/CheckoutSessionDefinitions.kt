package com.stripe.android.paymentsheet.example.playground.checkout.settings

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import com.stripe.android.paymentsheet.example.playground.settings.Currency as PlaygroundCurrency

internal object CheckoutSessionDefinitions {
    val backendUrl = optionalText(
        key = "session.backend_url",
        displayName = "Backend URL",
        validate = { value ->
            when {
                value.isBlank() -> null
                !value.startsWith("http://") && !value.startsWith("https://") -> "Must be a valid URL"
                !value.endsWith("/") -> "Must end with /"
                else -> null
            }
        },
    )
    val customer = choice(
        key = "session.customer",
        displayName = "Customer",
        defaultValue = "guest",
        options = listOf(
            "Guest" to "guest",
            "New" to "new",
            "Returning" to "returning",
        ),
        serialize = { it },
        updateRequest = { customer -> put("customer", customer) },
    )
    val customerId = optionalText(
        key = "session.customer_id",
        displayName = "Customer ID",
        updateRequest = { customerId ->
            customerId?.let { put("customer", it) }
        },
        validate = { null },
        isApplicable = { settings -> settings[customer] == RETURNING_CUSTOMER },
    )
    val paymentMethodSave = boolean(
        key = "session.payment_method_save",
        displayName = "Save payment methods",
        defaultValue = true,
        updateRequest = { enabled ->
            put("checkout_session_payment_method_save", if (enabled) "enabled" else "disabled")
        },
        isApplicable = { settings -> settings[customer] != GUEST_CUSTOMER },
    )
    val customerEmail = text(
        key = "session.customer_email",
        displayName = "Customer email",
        defaultValue = "email@example.com",
        updateRequest = { email -> put("customer_email", email) },
        validate = optionalEmail,
    )
    val currency = choice(
        key = "session.currency",
        displayName = "Currency",
        defaultValue = PlaygroundCurrency.USD,
        options = PlaygroundCurrency.entries.map {
            it.displayName to it
        },
        serialize = PlaygroundCurrency::value,
        updateRequest = { currency -> put("currency", currency.value) },
    )
    val automaticPaymentMethods = boolean(
        key = "session.automatic_payment_methods",
        displayName = "Automatic payment methods",
        defaultValue = true,
        updateRequest = { enabled -> put("automatic_payment_methods", enabled) },
    )
    val paymentMethodTypes = value(
        key = "session.payment_method_types",
        displayName = "Payment method types (comma separated)",
        defaultValue = listOf("card"),
        encode = { values -> values.joinToString(", ") },
        decode = { serialized ->
            Result.success(
                serialized.split(',')
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .distinct()
            )
        },
        updateRequest = { paymentMethodTypes ->
            put("payment_method_types", JsonArray(paymentMethodTypes.map(::JsonPrimitive)))
        },
        isApplicable = { settings -> !settings[automaticPaymentMethods] },
    )
    val automaticTax = boolean(
        key = "session.automatic_tax",
        displayName = "Automatic tax",
        defaultValue = false,
        updateRequest = { enabled ->
            put("automatic_tax", enabled)
            if (enabled) {
                put("merchant_country_code", "us_tax")
            }
        },
    )
    val shippingAddressCollection = boolean(
        key = "session.shipping_address_collection",
        displayName = "Collect shipping address",
        defaultValue = false,
        updateRequest = { enabled -> put("shipping_address_collection", enabled) },
    )
    val billingAddressCollection = boolean(
        key = "session.billing_address_collection",
        displayName = "Collect billing address",
        defaultValue = false,
        updateRequest = { enabled -> put("billing_address_collection", enabled) },
    )
    val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
        key = "session",
        displayName = "Checkout Session",
        children = arrayOf(
            backendUrl,
            customer,
            customerId,
            paymentMethodSave,
            customerEmail,
            currency,
            automaticPaymentMethods,
            paymentMethodTypes,
            automaticTax,
            shippingAddressCollection,
            billingAddressCollection,
        ),
    )

    private const val GUEST_CUSTOMER = "guest"
    private const val RETURNING_CUSTOMER = "returning"
}
