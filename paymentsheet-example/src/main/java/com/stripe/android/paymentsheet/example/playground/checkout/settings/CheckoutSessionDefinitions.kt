package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.stripe.android.paymentsheet.example.playground.applyFeatureFlags
import com.stripe.android.paymentsheet.example.playground.checkout.normalizedPlaygroundBaseUrl
import com.stripe.android.paymentsheet.example.playground.settings.LinkType
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import com.stripe.android.paymentsheet.example.playground.settings.Currency as PlaygroundCurrency

internal object CheckoutSessionDefinitions {
    val backendUrl = optionalText(
        key = "session.backend_url",
        displayName = "Backend URL",
        validate = { value ->
            if (value.isBlank()) null else runCatching { normalizedPlaygroundBaseUrl(value) }.exceptionOrNull()?.message
        },
    )
    val customer = choice(
        key = "session.customer",
        displayName = "Customer",
        options = CheckoutCustomer.entries.map { it.displayName to it },
        serialize = CheckoutCustomer::serializedValue,
    )
    val customerId = optionalText(
        key = "session.customer_id",
        displayName = "Customer ID",
        validate = { null },
        isApplicable = { settings -> settings[customer] == CheckoutCustomer.Returning },
    )
    val paymentMethodSave = boolean(
        key = "session.payment_method_save",
        displayName = "Save payment methods",
        defaultValue = true,
        isApplicable = { settings -> settings[customer] != CheckoutCustomer.Guest },
    )
    val paymentMethodRemove = boolean(
        key = "session.payment_method_remove",
        displayName = "Remove saved payment methods",
        defaultValue = true,
        isApplicable = { settings -> settings[customer] == CheckoutCustomer.Returning },
    )
    val customerEmail = text(
        key = "session.customer_email",
        displayName = "Customer email",
        defaultValue = "email@example.com",
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
    )
    val merchant = choice(
        key = "session.merchant",
        displayName = "Merchant",
        options = Merchant.entries.filter { it != Merchant.Custom }.map { it.name to it },
        serialize = Merchant::value,
    )
    val automaticPaymentMethods = boolean(
        key = "session.automatic_payment_methods",
        displayName = "Automatic payment methods",
        defaultValue = true,
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
        isApplicable = { settings -> !settings[automaticPaymentMethods] },
    )
    val automaticTax = boolean(
        key = "session.automatic_tax",
        displayName = "Automatic tax",
    )
    val adaptivePricingCountry = choice(
        key = "session.adaptive_pricing_country",
        displayName = "Adaptive pricing country",
        options = AdaptivePricingCountry.entries.map { it.displayName to it },
        serialize = AdaptivePricingCountry::serializedValue,
    )
    val shippingAddressCollection = boolean(
        key = "session.shipping_address_collection",
        displayName = "Collect shipping address",
    )
    val billingAddressCollection = boolean(
        key = "session.billing_address_collection",
        displayName = "Collect billing address",
    )
    val linkType = choice(
        key = "controller.link_type",
        displayName = "Link Type",
        options = LinkType.entries.map { it.value to it },
        serialize = LinkType::value,
        applyFeatureFlags = LinkType::applyFeatureFlags,
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
            linkType,
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
