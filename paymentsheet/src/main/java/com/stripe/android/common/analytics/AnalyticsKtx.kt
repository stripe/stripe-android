package com.stripe.android.common.analytics

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent
import com.stripe.android.uicore.StripeThemeDefaults

internal const val FIELD_APPEARANCE_USAGE = "usage"
internal const val FIELD_COLORS_LIGHT = "colorsLight"
internal const val FIELD_COLORS_DARK = "colorsDark"
internal const val FIELD_CORNER_RADIUS = "corner_radius"
internal const val FIELD_BORDER_WIDTH = "border_width"
internal const val FIELD_FONT = "font"
internal const val FIELD_SIZE_SCALE_FACTOR = "size_scale_factor"
internal const val FIELD_PRIMARY_BUTTON = "primary_button"
internal const val FIELD_ATTACH_DEFAULTS = "attach_defaults"
internal const val FIELD_COLLECT_NAME = "name"
internal const val FIELD_COLLECT_EMAIL = "email"
internal const val FIELD_COLLECT_PHONE = "phone"
internal const val FIELD_COLLECT_ADDRESS = "address"
internal const val FIELD_EMBEDDED_PAYMENT_ELEMENT = "embedded_payment_element"
internal const val FIELD_STYLE = "style"
internal const val FIELD_ROW_STYLE = "row_style"

internal fun PaymentSheet.Appearance.toAnalyticsMap(isEmbedded: Boolean = false): Map<String, Any?> {
    val primaryButtonConfig = primaryButton

    val primaryButtonConfigMap = mapOf(
        FIELD_COLORS_LIGHT to (primaryButton.colorsLight != PaymentSheet.PrimaryButtonColors.defaultLight),
        FIELD_COLORS_DARK to (primaryButton.colorsDark != PaymentSheet.PrimaryButtonColors.defaultDark),
        FIELD_CORNER_RADIUS to (primaryButtonConfig.shape.cornerRadiusDp != null),
        FIELD_BORDER_WIDTH to (primaryButtonConfig.shape.borderStrokeWidthDp != null),
        FIELD_FONT to (primaryButtonConfig.typography.fontResId != null)
    )

    val appearanceConfigMap = mutableMapOf<String, Any?>(
        FIELD_COLORS_LIGHT to (colorsLight != PaymentSheet.Colors.defaultLight),
        FIELD_COLORS_DARK to (colorsDark != PaymentSheet.Colors.defaultDark),
        FIELD_CORNER_RADIUS to (shapes.cornerRadiusDp != StripeThemeDefaults.shapes.cornerRadius),
        FIELD_BORDER_WIDTH to (shapes.borderStrokeWidthDp != StripeThemeDefaults.shapes.borderStrokeWidth),
        FIELD_FONT to (typography.fontResId != null),
        FIELD_SIZE_SCALE_FACTOR to (typography.sizeScaleFactor != StripeThemeDefaults.typography.fontSizeMultiplier),
        FIELD_PRIMARY_BUTTON to primaryButtonConfigMap
    )

    val embeddedConfigMap = embeddedAppearance.toAnalyticsMap()
    appearanceConfigMap[FIELD_EMBEDDED_PAYMENT_ELEMENT] = if (isEmbedded) embeddedConfigMap else null

    // We add a usage field to make queries easier.
    val usedPrimaryButtonApi = primaryButtonConfigMap.values.contains(true)
    val usedAppearanceApi = appearanceConfigMap.values.filterIsInstance<Boolean>().contains(true)
    val usedEmbeddedAppearanceApi = embeddedConfigMap.values.filterIsInstance<Boolean>().contains(true)

    appearanceConfigMap[FIELD_APPEARANCE_USAGE] = usedAppearanceApi || usedPrimaryButtonApi || usedEmbeddedAppearanceApi

    return appearanceConfigMap
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal fun PaymentSheet.Appearance.Embedded.toAnalyticsMap(): Map<String, Any?> {
    return mapOf(
        FIELD_STYLE to (this.style != PaymentSheet.Appearance.Embedded.default.style),
        FIELD_ROW_STYLE to this.style.toAnalyticsValue()
    )
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal fun PaymentSheet.Appearance.Embedded.RowStyle.toAnalyticsValue(): String {
    return when (this) {
        is PaymentSheet.Appearance.Embedded.RowStyle.FloatingButton -> "floating_button"
        is PaymentSheet.Appearance.Embedded.RowStyle.FlatWithRadio -> "flat_with_radio"
        is PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark -> "flat_with_checkmark"
    }
}

internal fun PaymentSheet.BillingDetailsCollectionConfiguration.toAnalyticsMap(): Map<String, Any?> {
    return mapOf(
        FIELD_ATTACH_DEFAULTS to attachDefaultsToPaymentMethod,
        FIELD_COLLECT_NAME to name.name,
        FIELD_COLLECT_EMAIL to email.name,
        FIELD_COLLECT_PHONE to phone.name,
        FIELD_COLLECT_ADDRESS to address.name,
    )
}

internal fun List<CardBrand>.toAnalyticsValue(): String? {
    return takeIf { brands ->
        brands.isNotEmpty()
    }?.joinToString { brand ->
        brand.code
    }
}

internal fun CommonConfiguration.getExternalPaymentMethodsAnalyticsValue(): List<String>? {
    return this.externalPaymentMethods.takeIf { it.isNotEmpty() }?.take(PaymentSheetEvent.MAX_EXTERNAL_PAYMENT_METHODS)
}

internal fun CommonConfiguration.getCustomPaymentMethodsAnalyticsValue(): List<String>? {
    return this.customPaymentMethods.takeIf { customPaymentMethods ->
        customPaymentMethods.isNotEmpty()
    }?.map { customPaymentMethod ->
        customPaymentMethod.id
    }
}

internal fun PaymentSheet.CardBrandAcceptance.toAnalyticsValue(): Boolean {
    return this !is PaymentSheet.CardBrandAcceptance.All
}

internal fun PaymentSheet.PaymentMethodLayout.toAnalyticsValue(): String {
    return when (this) {
        PaymentSheet.PaymentMethodLayout.Horizontal -> "horizontal"
        PaymentSheet.PaymentMethodLayout.Vertical -> "vertical"
        PaymentSheet.PaymentMethodLayout.Automatic -> "automatic"
    }
}
