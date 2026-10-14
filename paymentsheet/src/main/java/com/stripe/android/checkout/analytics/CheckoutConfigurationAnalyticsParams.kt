@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout.analytics

import com.stripe.android.checkout.CheckoutController

internal fun checkoutConfigurationAnalyticsParams(
    configuration: CheckoutController.Configuration.State,
): Map<String, Any?> = mapOf(
    "merchant_display_name" to (configuration.merchantDisplayName != null),
    "defaults" to mapOf(
        "billing_details" to mapOf(
            "name" to (configuration.defaults.billingDetails?.name != null),
            "address" to (configuration.defaults.billingDetails?.address != null),
        ),
        "shipping_details" to mapOf(
            "name" to (configuration.defaults.shippingDetails?.name != null),
            "address" to (configuration.defaults.shippingDetails?.address != null),
        ),
        "email" to (configuration.defaults.email != null),
        "phone" to (configuration.defaults.phone != null),
    ),
    "payment_element" to configuration.paymentElementConfiguration.let {
        mapOf(
            "embedded_view_displays_mandate_text" to it.embeddedViewDisplaysMandateText,
            "payment_method_layout" to it.paymentMethodLayout.name.lowercase(),
            "opens_card_scanner_automatically" to it.opensCardScannerAutomatically,
            "payment_method_order" to it.paymentMethodOrder,
            "preferred_networks" to it.preferredNetworks.map { network -> network.code },
            "link_display" to it.linkConfiguration.display.name.lowercase(),
            "google_pay_display" to it.googlePayConfiguration.display.name.lowercase(),
            "theme_mode" to it.appearance.themeMode.name.lowercase(),
        )
    },
    "express_checkout_element" to configuration.expressCheckoutElementConfiguration?.let {
        mapOf(
            "payment_method_order" to it.paymentMethodOrder.map { type -> type.name.lowercase() },
            "link_display" to it.linkConfiguration.display.name.lowercase(),
            "google_pay_display" to it.googlePayConfiguration.display.name.lowercase(),
        )
    },
    "currency_selector_element" to configuration.currencySelectorElementConfiguration?.let {
        mapOf("label_content" to it.appearance.labelContent.name.lowercase())
    },
    "shipping_address_element" to configuration.shippingAddressElementConfiguration?.let {
        mapOf(
            "title" to (it.title != null),
            "button_title" to (it.buttonTitle != null),
            "theme_mode" to it.appearance.themeMode.name.lowercase(),
        )
    },
)
