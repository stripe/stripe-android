@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout.analytics

import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.asPaymentSheet
import com.stripe.android.common.analytics.FIELD_COLORS_DARK
import com.stripe.android.common.analytics.FIELD_COLORS_LIGHT
import com.stripe.android.common.analytics.toAnalyticsMap
import com.stripe.android.elements.CheckoutGooglePayConfiguration
import com.stripe.android.elements.CurrencySelectorElement
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.elements.PaymentElement
import com.stripe.android.elements.ShippingAddressElement

internal fun CheckoutController.Configuration.State.toAnalyticsParams(): Map<String, Any?> = buildMap {
    put("merchant_display_name", merchantDisplayName != null)
    put("defaults", defaults.toAnalyticsParams())
    put("payment_element", paymentElementConfiguration?.toAnalyticsParams())
    put("express_checkout_element", expressCheckoutElementConfiguration?.toAnalyticsParams())
    put("currency_selector_element", currencySelectorElementConfiguration?.toAnalyticsParams())
    put("shipping_address_element", shippingAddressElementConfiguration?.toAnalyticsParams())
}

private fun CheckoutController.Configuration.Defaults.State.toAnalyticsParams(): Map<String, Any?> = mapOf(
    "billing_details" to mapOf(
        "name" to (billingDetails?.name != null),
        "address" to (billingDetails?.address != null),
    ),
    "shipping_details" to mapOf(
        "name" to (shippingDetails?.name != null),
        "address" to (shippingDetails?.address != null),
    ),
    "email" to (email != null),
    "phone" to (phone != null),
)

private fun PaymentElement.Configuration.State.toAnalyticsParams(): Map<String, Any?> = mapOf(
    "embedded_view_displays_mandate_text" to embeddedViewDisplaysMandateText,
    "payment_method_layout" to paymentMethodLayout.name.lowercase(),
    "opens_card_scanner_automatically" to opensCardScannerAutomatically,
    "preferred_networks" to preferredNetworks.map { it.code },
    "payment_method_order" to paymentMethodOrder.toList(),
    "card_brand_acceptance" to cardBrandAcceptance.toAnalyticsParams(),
    "terms_display" to buildMap {
        termsDisplay.forEach { (paymentMethodType, termsDisplayValue) ->
            put(paymentMethodType.code, termsDisplayValue.name.lowercase())
        }
    },
    "appearance" to appearance.asPaymentSheet().toAnalyticsMap(isEmbedded = true).withSnakeCaseColorKeys(),
    "google_pay_configuration" to googlePayConfiguration.toAnalyticsParams(),
    "link_configuration" to mapOf(
        "display" to linkConfiguration.display.toAnalyticsValue(),
    ),
)

private fun PaymentElement.Configuration.CardBrandAcceptance.toAnalyticsParams(): Map<String, Any?> = when (this) {
    is PaymentElement.Configuration.CardBrandAcceptance.All -> mapOf(
        "type" to "all",
        "brands" to emptyList<String>(),
    )
    is PaymentElement.Configuration.CardBrandAcceptance.Allowed -> mapOf(
        "type" to "allowed",
        "brands" to brands.map { it.name.lowercase() },
    )
    is PaymentElement.Configuration.CardBrandAcceptance.Disallowed -> mapOf(
        "type" to "disallowed",
        "brands" to brands.map { it.name.lowercase() },
    )
}

private fun CheckoutGooglePayConfiguration.toAnalyticsParams(): Map<String, Any?> = mapOf(
    "display" to display.name.lowercase(),
    "label" to (label != null),
    "button_type" to buttonType.name.lowercase(),
    "additional_enabled_networks" to additionalEnabledNetworks.toList(),
)

private fun PaymentElement.Configuration.LinkConfiguration.Display.toAnalyticsValue(): String = when (this) {
    PaymentElement.Configuration.LinkConfiguration.Display.Automatic -> "automatic"
    PaymentElement.Configuration.LinkConfiguration.Display.Never -> "never"
    PaymentElement.Configuration.LinkConfiguration.Display.WalletButtonHidden -> "wallet_button_hidden"
}

private fun ExpressCheckoutElement.Configuration.State.toAnalyticsParams(): Map<String, Any?> = mapOf(
    "link_configuration" to mapOf(
        "display" to linkConfiguration.display.name.lowercase(),
    ),
    "google_pay_configuration" to googlePayConfiguration.toAnalyticsParams(),
    "appearance" to mapOf(
        "button_layout" to mapOf(
            "max_columns" to appearance.buttonLayout.maxColumns,
            "max_rows" to appearance.buttonLayout.maxRows,
        ),
        "button_theme" to appearance.buttonTheme.name.lowercase(),
        "corner_radius_dp" to appearance.cornerRadiusDp,
    ),
)

private fun CurrencySelectorElement.Configuration.State.toAnalyticsParams(): Map<String, Any?> = mapOf(
    "appearance" to appearance.toAnalyticsParams(),
)

private fun CurrencySelectorElement.Configuration.Appearance.State.toAnalyticsParams(): Map<String, Any?> = mapOf(
    "content_vertical_padding_dp" to contentVerticalPaddingDp,
    "corner_radius_dp" to cornerRadiusDp,
    "border_width_dp" to borderWidthDp,
    "border_color" to borderColor,
    "background" to background,
    "selected_background" to selectedBackground,
    "text_color" to textColor,
    "selected_text_color" to selectedTextColor,
    "text_secondary_color" to textSecondaryColor,
    "danger_color" to dangerColor,
    "font_res_id" to fontResId,
    "size_scale_factor" to sizeScaleFactor,
    "label_content" to labelContent.name.lowercase(),
)

private fun ShippingAddressElement.Configuration.State.toAnalyticsParams(): Map<String, Any?> = mapOf(
    "title" to (title != null),
    "button_title" to (buttonTitle != null),
    "appearance" to appearance.asPaymentSheet().toAnalyticsMap().withSnakeCaseColorKeys(),
)

private fun Map<*, *>.withSnakeCaseColorKeys(): Map<String, Any?> = entries.associate { (key, value) ->
    val analyticsKey = when (key) {
        FIELD_COLORS_LIGHT -> "colors_light"
        FIELD_COLORS_DARK -> "colors_dark"
        else -> key as String
    }
    analyticsKey to if (value is Map<*, *>) value.withSnakeCaseColorKeys() else value
}
