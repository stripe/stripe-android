package com.stripe.android.paymentsheet

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.PaymentsThemeDefaults
import com.stripe.android.ui.core.PrimaryButtonColors
import com.stripe.android.ui.core.PrimaryButtonShape
import com.stripe.android.ui.core.PrimaryButtonTypography
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
        component = Color(colorsLight.component),
        componentBorder = Color(colorsLight.componentBorder),
        componentDivider = Color(colorsLight.componentDivider),
        onComponent = Color(colorsLight.onComponent),
        subtitle = Color(colorsLight.subtitle),
        placeholderText = Color(colorsLight.placeholderText),
        appBarIcon = Color(colorsLight.appBarIcon),
        materialColors = lightColors(
            primary = Color(colorsLight.primary),
            surface = Color(colorsLight.surface),
            onSurface = Color(colorsLight.onSurface),
            error = Color(colorsLight.error)
        )
    )

    PaymentsTheme.colorsDarkMutable = PaymentsThemeDefaults.colorsDark.copy(
        component = Color(colorsDark.component),
        componentBorder = Color(colorsDark.componentBorder),
        componentDivider = Color(colorsDark.componentDivider),
        onComponent = Color(colorsDark.onComponent),
        subtitle = Color(colorsDark.subtitle),
        placeholderText = Color(colorsDark.placeholderText),
        appBarIcon = Color(colorsDark.appBarIcon),
        materialColors = darkColors(
            primary = Color(colorsDark.primary),
            surface = Color(colorsDark.surface),
            onSurface = Color(colorsDark.onSurface),
            error = Color(colorsDark.error)
        )
    )

    PaymentsTheme.shapesMutable = PaymentsThemeDefaults.shapes.copy(
        cornerRadius = shapes.cornerRadiusDp,
        borderStrokeWidth = shapes.borderStrokeWidthDp
    )

    PaymentsTheme.typographyMutable = PaymentsThemeDefaults.typography.copy(
        fontFamily = typography.fontResId,
        fontSizeMultiplier = typography.sizeScaleFactor
    )

    PaymentsTheme.primaryButtonStyle = PaymentsThemeDefaults.primaryButtonStyle.copy(
        colorsLight = PrimaryButtonColors(
            background = Color(primaryButton.colorsLight.background ?: colorsLight.primary),
            onBackground = Color(primaryButton.colorsLight.onBackground),
            border = Color(primaryButton.colorsLight.border),
        ),
        colorsDark = PrimaryButtonColors(
            background = Color(primaryButton.colorsDark.background ?: colorsDark.primary),
            onBackground = Color(primaryButton.colorsDark.onBackground),
            border = Color(primaryButton.colorsDark.border),
        ),
        shape = PrimaryButtonShape(
            cornerRadius = primaryButton.shape.cornerRadiusDp ?: shapes.cornerRadiusDp,
            borderStrokeWidth =
            primaryButton.shape.borderStrokeWidthDp ?: shapes.borderStrokeWidthDp,
        ),
        typography = PrimaryButtonTypography(
            fontFamily = primaryButton.typography.fontResId ?: typography.fontResId,
            fontSize = primaryButton.typography.fontSizeSp?.sp
                ?: (PaymentsThemeDefaults.typography.largeFontSize * typography.sizeScaleFactor)
        )
    )
}
