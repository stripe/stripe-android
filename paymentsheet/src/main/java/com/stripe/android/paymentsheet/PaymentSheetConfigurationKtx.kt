package com.stripe.android.paymentsheet

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.stripe.android.uicore.PrimaryButtonColors
import com.stripe.android.uicore.PrimaryButtonShape
import com.stripe.android.uicore.PrimaryButtonTypography
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.StripeThemeDefaults

internal fun PaymentSheet.Appearance.parseAppearance() {
    StripeTheme.colorsLightMutable = StripeThemeDefaults.colorsLight.copy(
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

    StripeTheme.colorsDarkMutable = StripeThemeDefaults.colorsDark.copy(
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

    StripeTheme.shapesMutable = StripeThemeDefaults.shapes.copy(
        cornerRadius = shapes.cornerRadiusDp,
        borderStrokeWidth = shapes.borderStrokeWidthDp
    )

    StripeTheme.typographyMutable = StripeThemeDefaults.typography.copy(
        fontFamily = typography.fontResId,
        fontSizeMultiplier = typography.sizeScaleFactor
    )

    StripeTheme.primaryButtonStyle = StripeThemeDefaults.primaryButtonStyle.copy(
        colorsLight = PrimaryButtonColors(
            background = Color(primaryButton.colorsLight.background ?: colorsLight.primary),
            onBackground = Color(primaryButton.colorsLight.onBackground),
            border = Color(primaryButton.colorsLight.border),
            successBackground = Color(primaryButton.colorsLight.successBackgroundColor),
            onSuccessBackground = Color(primaryButton.colorsLight.onSuccessBackgroundColor),
        ),
        colorsDark = PrimaryButtonColors(
            background = Color(primaryButton.colorsDark.background ?: colorsDark.primary),
            onBackground = Color(primaryButton.colorsDark.onBackground),
            border = Color(primaryButton.colorsDark.border),
            successBackground = Color(primaryButton.colorsDark.successBackgroundColor),
            onSuccessBackground = Color(primaryButton.colorsDark.onSuccessBackgroundColor),
        ),
        shape = PrimaryButtonShape(
            cornerRadius = primaryButton.shape.cornerRadiusDp ?: shapes.cornerRadiusDp,
            borderStrokeWidth =
            primaryButton.shape.borderStrokeWidthDp ?: shapes.borderStrokeWidthDp
        ),
        typography = PrimaryButtonTypography(
            fontFamily = primaryButton.typography.fontResId ?: typography.fontResId,
            fontSize = primaryButton.typography.fontSizeSp?.sp
                ?: (StripeThemeDefaults.typography.largeFontSize * typography.sizeScaleFactor)
        )
    )
}
