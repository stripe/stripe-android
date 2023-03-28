@file:Suppress("MagicNumber")

package com.stripe.android.financialconnections.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

internal val Info100 = Color(0xffCFF5F6)
internal val Success100 = Color(0xffD7F7C2)

internal val Neutral50 = Color(0xffF6F8FA)
internal val Neutral150 = Color(0xffE0E6EB)
internal val Neutral200 = Color(0xffC0C8D2)
internal val Neutral300 = Color(0xffA3ACBA)
internal val Neutral500 = Color(0xff6A7383)
internal val Neutral800 = Color(0xff30313D)

internal val Attention500 = Color(0xffC84801)
internal val Attention400 = Color(0xffED6704)
internal val Attention100 = Color(0xffFCEDB9)
internal val Attention50 = Color(0xffFEF9DA)

internal val Brand100 = Color(0xffF2EBFF)
internal val Brand400 = Color(0xff8D7FFA)
internal val Brand500 = Color(0xff625AFA)

internal val Blue500 = Color(0xff0570DE)
internal val Blue400 = Color(0xFF0196ED)

internal val Red500 = Color(0xffDF1B41)

internal val Green400 = Color(0xff3FA40D)
internal val Green500 = Color(0xff228403)

/**
 * Financial Connections custom Color Palette
 */
@Immutable
internal data class FinancialConnectionsColors(
    // backgrounds
    val backgroundSurface: Color,
    val backgroundContainer: Color,
    val backgroundBackdrop: Color,
    // borders
    val borderDefault: Color,
    val borderFocus: Color,
    val borderInvalid: Color,
    // text
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val textWhite: Color,
    val textBrand: Color,
    val textInfo: Color,
    val textSuccess: Color,
    val textAttention: Color,
    val textCritical: Color,
    // icons
    val iconBrand: Color,
    val iconInfo: Color,
    val iconSuccess: Color,
    val iconAttention: Color
)
