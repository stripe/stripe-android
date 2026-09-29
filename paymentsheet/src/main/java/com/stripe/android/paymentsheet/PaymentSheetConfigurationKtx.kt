package com.stripe.android.paymentsheet

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.uicore.StripeTheme

@OptIn(AppearanceAPIAdditionsPreview::class)
internal fun PaymentSheet.Appearance.parseAppearance() {
    val values = toPaymentElementThemeValues()

    StripeTheme.colorsLightMutable = values.colorsLight
    StripeTheme.colorsDarkMutable = values.colorsDark
    StripeTheme.shapesMutable = values.shapes
    StripeTheme.typographyMutable = values.typography
    StripeTheme.primaryButtonStyle = values.primaryButtonStyle
    StripeTheme.formInsets = values.formInsets
    StripeTheme.customSectionSpacing = values.sectionSpacing
    StripeTheme.verticalModeRowPadding = values.verticalModeRowPadding
    StripeTheme.textFieldInsets = values.textFieldInsets
    StripeTheme.iconStyle = values.iconStyle
}

internal val WalletType.configType: PaymentSheet.WalletButtonsConfiguration.Wallet
    get() = when (this) {
        WalletType.Link -> PaymentSheet.WalletButtonsConfiguration.Wallet.Link
        WalletType.GooglePay -> PaymentSheet.WalletButtonsConfiguration.Wallet.GooglePay
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
