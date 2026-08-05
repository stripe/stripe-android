package com.stripe.android.paymentsheet

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.uicore.FormInsets
import com.stripe.android.uicore.PrimaryButtonColors
import com.stripe.android.uicore.PrimaryButtonShape
import com.stripe.android.uicore.PrimaryButtonStyle
import com.stripe.android.uicore.PrimaryButtonTypography
import com.stripe.android.uicore.StripeColors
import com.stripe.android.uicore.StripeShapes
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.StripeTypography
import com.stripe.android.uicore.IconStyle as StripeIconStyle

internal data class PaymentElementThemeValues(
    val colorsLight: StripeColors,
    val colorsDark: StripeColors,
    val shapes: StripeShapes,
    val typography: StripeTypography,
    val primaryButtonStyle: PrimaryButtonStyle,
    val formInsets: FormInsets,
    val sectionSpacing: Float?,
    val textFieldInsets: FormInsets,
    val iconStyle: StripeIconStyle,
    val verticalModeRowPadding: Float,
)

@OptIn(AppearanceAPIAdditionsPreview::class)
internal fun PaymentSheet.Appearance.toPaymentElementThemeValues(): PaymentElementThemeValues {
    return PaymentElementThemeValues(
        colorsLight = colorsLight.toStripeColors(isDark = false),
        colorsDark = colorsDark.toStripeColors(isDark = true),
        shapes = shapes.toStripeShapes(),
        typography = typography.toStripeTypography(),
        primaryButtonStyle = primaryButton.toStripePrimaryButtonStyle(
            colorsLight = colorsLight,
            colorsDark = colorsDark,
            shapes = shapes,
            typography = typography,
        ),
        formInsets = formInsetValues.toStripeFormInsets(),
        sectionSpacing = sectionSpacing.spacingDp.takeIf { it >= 0f },
        textFieldInsets = textFieldInsets.toStripeFormInsets(),
        iconStyle = iconStyle.toStripeIconStyle(),
        verticalModeRowPadding = verticalModeRowPadding,
    )
}

private fun PaymentSheet.Colors.toStripeColors(isDark: Boolean): StripeColors {
    val materialColors = if (isDark) {
        darkColors(
            primary = Color(primary),
            surface = Color(surface),
            onSurface = Color(onSurface),
            error = Color(error),
        )
    } else {
        lightColors(
            primary = Color(primary),
            surface = Color(surface),
            onSurface = Color(onSurface),
            error = Color(error),
        )
    }

    return StripeThemeDefaults.colors(isDark).copy(
        component = Color(component),
        componentBorder = Color(componentBorder),
        componentDivider = Color(componentDivider),
        onComponent = Color(onComponent),
        subtitle = Color(subtitle),
        placeholderText = Color(placeholderText),
        appBarIcon = Color(appBarIcon),
        materialColors = materialColors,
    )
}

private fun PaymentSheet.Shapes.toStripeShapes(): StripeShapes {
    return StripeThemeDefaults.shapes.copy(
        cornerRadius = cornerRadiusDp,
        bottomSheetCornerRadius = bottomSheetCornerRadiusDp,
        borderStrokeWidth = borderStrokeWidthDp,
    )
}

@OptIn(AppearanceAPIAdditionsPreview::class)
private fun PaymentSheet.Typography.toStripeTypography(): StripeTypography {
    return StripeThemeDefaults.typography.copy(
        fontFamily = fontResId,
        fontSizeMultiplier = sizeScaleFactor,
        h4 = custom.h1?.toTextStyle(),
    )
}

private fun PaymentSheet.PrimaryButton.toStripePrimaryButtonStyle(
    colorsLight: PaymentSheet.Colors,
    colorsDark: PaymentSheet.Colors,
    shapes: PaymentSheet.Shapes,
    typography: PaymentSheet.Typography,
): PrimaryButtonStyle {
    return StripeThemeDefaults.primaryButtonStyle.copy(
        colorsLight = this.colorsLight.toStripePrimaryButtonColors(fallbackBackground = colorsLight.primary),
        colorsDark = this.colorsDark.toStripePrimaryButtonColors(fallbackBackground = colorsDark.primary),
        shape = PrimaryButtonShape(
            cornerRadius = shape.cornerRadiusDp ?: shapes.cornerRadiusDp,
            borderStrokeWidth = shape.borderStrokeWidthDp ?: shapes.borderStrokeWidthDp,
            height = shape.heightDp ?: StripeThemeDefaults.primaryButtonStyle.shape.height,
        ),
        typography = PrimaryButtonTypography(
            fontFamily = this.typography.fontResId ?: typography.fontResId,
            fontSize = this.typography.fontSizeSp?.sp
                ?: (StripeThemeDefaults.typography.largeFontSize * typography.sizeScaleFactor),
        ),
    )
}

private fun PaymentSheet.PrimaryButtonColors.toStripePrimaryButtonColors(
    fallbackBackground: Int,
): PrimaryButtonColors {
    return PrimaryButtonColors(
        background = Color(background ?: fallbackBackground),
        onBackground = Color(onBackground),
        border = Color(border),
        successBackground = Color(successBackgroundColor),
        onSuccessBackground = Color(onSuccessBackgroundColor),
    )
}

private fun PaymentSheet.Insets.toStripeFormInsets(): FormInsets {
    return FormInsets(
        start = startDp,
        top = topDp,
        end = endDp,
        bottom = bottomDp,
    )
}

@OptIn(AppearanceAPIAdditionsPreview::class)
private fun PaymentSheet.IconStyle.toStripeIconStyle(): StripeIconStyle {
    return when (this) {
        PaymentSheet.IconStyle.Filled -> StripeIconStyle.Filled
        PaymentSheet.IconStyle.Outlined -> StripeIconStyle.Outlined
    }
}
