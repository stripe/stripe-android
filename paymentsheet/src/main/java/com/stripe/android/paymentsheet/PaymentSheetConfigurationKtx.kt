package com.stripe.android.paymentsheet

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.uicore.FormInsets
import com.stripe.android.uicore.IconStyle
import com.stripe.android.uicore.PrimaryButtonColors
import com.stripe.android.uicore.PrimaryButtonShape
import com.stripe.android.uicore.PrimaryButtonTypography
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.StripeThemeDefaults

@OptIn(AppearanceAPIAdditionsPreview::class)
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
        bottomSheetCornerRadius = shapes.bottomSheetCornerRadiusDp,
        borderStrokeWidth = shapes.borderStrokeWidthDp
    )

    StripeTheme.typographyMutable = StripeThemeDefaults.typography.copy(
        fontFamily = typography.fontResId,
        fontSizeMultiplier = typography.sizeScaleFactor,
        h4 = typography.custom.h1?.toTextStyle(),
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
            primaryButton.shape.borderStrokeWidthDp ?: shapes.borderStrokeWidthDp,
            height = primaryButton.shape.heightDp ?: StripeThemeDefaults.primaryButtonStyle.shape.height
        ),
        typography = PrimaryButtonTypography(
            fontFamily = primaryButton.typography.fontResId ?: typography.fontResId,
            fontSize = primaryButton.typography.fontSizeSp?.sp
                ?: (StripeThemeDefaults.typography.largeFontSize * typography.sizeScaleFactor)
        )
    )

    StripeTheme.formInsets = StripeThemeDefaults.formInsets.copy(
        start = formInsetValues.startDp,
        top = formInsetValues.topDp,
        end = formInsetValues.endDp,
        bottom = formInsetValues.bottomDp
    )

    StripeTheme.customSectionSpacing = if (sectionSpacing.spacingDp >= 0f) {
        sectionSpacing.spacingDp
    } else {
        null
    }

    StripeTheme.verticalModeRowPadding = verticalModeRowPadding

    StripeTheme.textFieldInsets = FormInsets(
        start = textFieldInsets.startDp,
        end = textFieldInsets.endDp,
        top = textFieldInsets.topDp,
        bottom = textFieldInsets.bottomDp,
    )

    StripeTheme.iconStyle = when (iconStyle) {
        PaymentSheet.IconStyle.Filled -> IconStyle.Filled
        PaymentSheet.IconStyle.Outlined -> IconStyle.Outlined
    }
}

internal val PaymentSheet.WalletButtonsConfiguration.allowedWalletTypes: List<WalletType>
    get() = if (walletsToShow.isEmpty()) {
        WalletType.entries
    } else {
        WalletType.entries.filter { type ->
            walletsToShow.contains(type.code)
        }
    }

@OptIn(AppearanceAPIAdditionsPreview::class)
internal fun PaymentSheet.Typography.Font.toTextStyle(): TextStyle {
    return TextStyle(
        fontSize = fontSizeSp?.sp ?: TextUnit.Unspecified,
        fontWeight = fontWeight?.let { FontWeight(it) },
        fontFamily = fontFamily?.let { FontFamily(Font(it)) },
        letterSpacing = letterSpacingSp?.sp ?: TextUnit.Unspecified,
    )
}
