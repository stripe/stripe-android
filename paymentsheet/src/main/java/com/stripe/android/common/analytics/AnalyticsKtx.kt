package com.stripe.android.common.analytics

import com.stripe.android.model.CardBrand
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

internal fun PaymentSheet.Appearance.toAnalyticsMap(): Map<String, Any?> {
    val primaryButtonConfig = primaryButton

    val primaryButtonConfigMap = mapOf(
        FIELD_COLORS_LIGHT to (primaryButton.colorsLight != PaymentSheet.PrimaryButtonColors.defaultLight),
        FIELD_COLORS_DARK to (primaryButton.colorsDark != PaymentSheet.PrimaryButtonColors.defaultDark),
        FIELD_CORNER_RADIUS to (primaryButtonConfig.shape.cornerRadiusDp != null),
        FIELD_BORDER_WIDTH to (primaryButtonConfig.shape.borderStrokeWidthDp != null),
        FIELD_FONT to (primaryButtonConfig.typography.fontResId != null)
    )

    val appearanceConfigMap = mutableMapOf(
        FIELD_COLORS_LIGHT to (colorsLight != PaymentSheet.Colors.defaultLight),
        FIELD_COLORS_DARK to (colorsDark != PaymentSheet.Colors.defaultDark),
        FIELD_CORNER_RADIUS to (shapes.cornerRadiusDp != StripeThemeDefaults.shapes.cornerRadius),
        FIELD_BORDER_WIDTH to (shapes.borderStrokeWidthDp != StripeThemeDefaults.shapes.borderStrokeWidth),
        FIELD_FONT to (typography.fontResId != null),
        FIELD_SIZE_SCALE_FACTOR to (typography.sizeScaleFactor != StripeThemeDefaults.typography.fontSizeMultiplier),
        FIELD_PRIMARY_BUTTON to primaryButtonConfigMap
    )

    // We add a usage field to make queries easier.
    val usedPrimaryButtonApi = primaryButtonConfigMap.values.contains(true)
    val usedAppearanceApi = appearanceConfigMap.values.filterIsInstance<Boolean>().contains(true)

    appearanceConfigMap[FIELD_APPEARANCE_USAGE] = usedAppearanceApi || usedPrimaryButtonApi

    return appearanceConfigMap
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

internal fun PaymentSheet.Configuration.getExternalPaymentMethodsAnalyticsValue(): List<String>? {
    return this.externalPaymentMethods.takeIf { it.isNotEmpty() }?.take(PaymentSheetEvent.MAX_EXTERNAL_PAYMENT_METHODS)
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
