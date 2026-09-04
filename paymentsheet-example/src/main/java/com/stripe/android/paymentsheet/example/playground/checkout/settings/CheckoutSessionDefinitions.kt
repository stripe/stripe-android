package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.stripe.android.paymentsheet.example.playground.settings.Merchant
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
        defaultValue = CheckoutCustomer.Guest,
        options = CheckoutCustomer.entries.map { it.displayName to it },
        serialize = CheckoutCustomer::serializedValue,
        updateRequest = { customer -> put("customer", customer.serializedValue) },
    )
    val customerId = optionalText(
        key = "session.customer_id",
        displayName = "Customer ID",
        updateRequest = { customerId ->
            customerId?.let { put("customer", it) }
        },
        validate = { null },
        isApplicable = { settings -> settings[customer] == CheckoutCustomer.Returning },
    )
    val paymentMethodSave = boolean(
        key = "session.payment_method_save",
        displayName = "Save payment methods",
        defaultValue = true,
        updateRequest = { enabled ->
            put("checkout_session_payment_method_save", if (enabled) "enabled" else "disabled")
        },
        isApplicable = { settings -> settings[customer] != CheckoutCustomer.Guest },
    )
    val paymentMethodRemove = boolean(
        key = "session.payment_method_remove",
        displayName = "Remove saved payment methods",
        defaultValue = true,
        updateRequest = { enabled ->
            put("checkout_session_payment_method_remove", if (enabled) "enabled" else "disabled")
        },
        isApplicable = { settings -> settings[customer] == CheckoutCustomer.Returning },
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
    val merchant = choice(
        key = "session.merchant",
        displayName = "Merchant",
        defaultValue = Merchant.US,
        options = Merchant.entries.filter { it != Merchant.Custom }.map { it.name to it },
        serialize = Merchant::value,
        updateRequest = { merchant -> put("merchant_country_code", merchant.value) },
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
        },
    )
    val adaptivePricingCountry = choice(
        key = "session.adaptive_pricing_country",
        displayName = "Adaptive pricing country",
        defaultValue = AdaptivePricingCountry.None,
        options = AdaptivePricingCountry.entries.map { it.displayName to it },
        serialize = AdaptivePricingCountry::serializedValue,
        updateRequest = { country ->
            country.countryCode?.let { countryCode ->
                put("adaptive_pricing", true)
                put("customer_email", "test+location_$countryCode@example.com")
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
            paymentMethodRemove,
            customerEmail,
            currency,
            merchant,
            automaticPaymentMethods,
            paymentMethodTypes,
            automaticTax,
            adaptivePricingCountry,
            shippingAddressCollection,
            billingAddressCollection,
        ),
    )
}

internal enum class CheckoutCustomer(
    val displayName: String,
    val serializedValue: String,
) {
    Guest("Guest", "guest"),
    New("New", "new"),
    Returning("Returning", "returning"),
}

internal enum class AdaptivePricingCountry(
    val displayName: String,
    val serializedValue: String,
    val countryCode: String?,
) {
    None("Off", "", null),
    France("France", "FR", "FR"),
    Japan("Japan", "JP", "JP"),
}
