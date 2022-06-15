package com.stripe.android.financialconnections.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

val Neutral50 = Color(0xffF6F8FA)
val Neutral300 = Color(0xffA3ACBA)
val Neutral500 = Color(0xff6A7383)
val Neutral800 = Color(0xff30313D)

val Blurple500 = Color(0xff635BFF)

val Blue500 = Color(0xff0570DE)

val Red500 = Color(0xffDF1B41)

val Green500 = Color(0xff228403)

/**
 * Financial Connections custom Color Palette
 */
@Immutable
data class FinancialConnectionsColors(
    val backgroundSurface: Color,
    val backgroundContainer: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val textWhite: Color,
    val textBrand: Color,
    val textInfo: Color,
    val textSuccess: Color,
    val textAttention: Color,
    val textCritical: Color
)
