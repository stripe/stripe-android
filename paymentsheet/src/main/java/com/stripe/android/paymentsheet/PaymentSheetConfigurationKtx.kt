package com.stripe.android.paymentsheet

import androidx.compose.ui.graphics.Color
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.PaymentsThemeDefaults
import java.security.InvalidParameterException

internal fun PaymentSheet.Configuration.validate() {
    // These are not localized as they are not intended to be displayed to a user.
    when {
        merchantDisplayName.isBlank() -> {
            throw InvalidParameterException(
                "When a Configuration is passed to PaymentSheet," +
                    " the Merchant display name cannot be an empty string."
            )
        }
        customer?.id?.isBlank() == true -> {
            throw InvalidParameterException(
                "When a CustomerConfiguration is passed to PaymentSheet," +
                    " the Customer ID cannot be an empty string."
            )
        }
        customer?.ephemeralKeySecret?.isBlank() == true -> {
            throw InvalidParameterException(
                "When a CustomerConfiguration is passed to PaymentSheet, " +
                    "the ephemeralKeySecret cannot be an empty string."
            )
        }
    }
}

internal fun PaymentSheet.Appearance.parseAppearance() {
    PaymentsTheme.colorsLightMutable = PaymentsThemeDefaults.colorsLight.copy(
        primary = Color(colorsLight.primary),
        surface = Color(colorsLight.surface),
        component = Color(colorsLight.component),
        componentBorder = Color(colorsLight.componentBorder),
        componentDivider = Color(colorsLight.componentDivider),
        onComponent = Color(colorsLight.onComponent),
        subtitle = Color(colorsLight.subtitle),
        placeholderText = Color(colorsLight.placeholderText),
        onSurface = Color(colorsLight.onSurface),
        appBarIcon = Color(colorsLight.appBarIcon),
        error = Color(colorsLight.error)
    )

    PaymentsTheme.colorsDarkMutable = PaymentsThemeDefaults.colorsDark.copy(
        primary = Color(colorsDark.primary),
        surface = Color(colorsDark.surface),
        component = Color(colorsDark.component),
        componentBorder = Color(colorsDark.componentBorder),
        componentDivider = Color(colorsDark.componentDivider),
        onComponent = Color(colorsDark.onComponent),
        subtitle = Color(colorsDark.subtitle),
        placeholderText = Color(colorsDark.placeholderText),
        onSurface = Color(colorsDark.onSurface),
        appBarIcon = Color(colorsDark.appBarIcon),
        error = Color(colorsDark.error)
    )

    PaymentsTheme.shapesMutable = PaymentsThemeDefaults.shapes.copy(
        cornerRadius = shapes.cornerRadiusDp,
        borderStrokeWidth = shapes.borderStrokeWidthDp
    )

    PaymentsTheme.typographyMutable = PaymentsThemeDefaults.typography.copy(
        fontFamily = typography.fontResId,
        fontWeightNormal = typography.normalWeight,
        fontWeightMedium = typography.mediumWeight,
        fontWeightBold = typography.boldWeight,
        fontSizeMultiplier = typography.sizeScaleFactor
    )
}
