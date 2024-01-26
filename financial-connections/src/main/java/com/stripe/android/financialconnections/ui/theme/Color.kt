@file:Suppress("MagicNumber")

package com.stripe.android.financialconnections.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Typography

internal val Neutral50 = Color(0xffF6F8FA)
internal val Neutral150 = Color(0xffE0E6EB)
internal val Neutral200 = Color(0xffC0C8D2)
internal val Neutral300 = Color(0xffA3ACBA)
internal val Neutral500 = Color(0xff6A7383)
internal val Neutral800 = Color(0xff30313D)
internal val Neutral900 = Color(0xff21252C)

internal val Attention500 = Color(0xffC84801)
internal val Attention400 = Color(0xffED6704)
internal val Attention100 = Color(0xffFCEDB9)
internal val Attention50 = Color(0xffFEF9DA)

internal val Brand50 = Color(0xffF7F5FD)
internal val Brand100 = Color(0xffF2EBFF)
internal val Brand400 = Color(0xff8D7FFA)
internal val Brand500 = Color(0xff625AFA)

internal val Blue500 = Color(0xff0570DE)
internal val Blue400 = Color(0xFF0196ED)

internal val Red500 = Color(0xffDF1B41)

internal val Green400 = Color(0xff3FA40D)
internal val Green500 = Color(0xff228403)

internal object LinkColors {
    val Brand200 = Color(0xffA6FBDD)
    val Brand600 = Color(0xff1AC59B)
}

/**
 * Financial Connections custom Color Palette
 */
@Immutable
@Deprecated("Use FinancialConnectionsV3Colors")
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

@Immutable
internal data class FinancialConnectionsV3Colors(
    val textDefault: Color,
    val textSubdued: Color,
    val textDisabled: Color,
    val textWhite: Color,
    val textBrand: Color,
    val textCritical: Color,
    val iconDefault: Color,
    val iconSubdued: Color,
    val iconWhite: Color,
    val iconBrand: Color,
    val buttonPrimary: Color,
    val buttonPrimaryHover: Color,
    val buttonPrimaryPressed: Color,
    val buttonSecondary: Color,
    val buttonSecondaryHover: Color,
    val buttonSecondaryPressed: Color,
    val backgroundSurface: Color,
    val background: Color,
    val backgroundOffset: Color,
    val backgroundBrand: Color,
    val border: Color,
    val borderBrand: Color
)

@Preview(group = "Components", name = "Colors")
@Composable
internal fun ColorsPreview() {
    FinancialConnectionsPreview {
        Column(
            modifier = Modifier.background(Color.White)
        ) {
            ColorPreview("textDefault", v3Colors.textDefault)
            ColorPreview("textSubdued", v3Colors.textSubdued)
            ColorPreview("textDisabled", v3Colors.textDisabled)
            ColorPreview("textWhite", v3Colors.textWhite)
            ColorPreview("textBrand", v3Colors.textBrand)
            ColorPreview("textCritical", v3Colors.textCritical)
            ColorPreview("iconDefault", v3Colors.iconDefault)
            ColorPreview("iconSubdued", v3Colors.iconSubdued)
            ColorPreview("iconWhite", v3Colors.iconWhite)
            ColorPreview("iconBrand", v3Colors.iconBrand)
            ColorPreview("buttonPrimary", v3Colors.buttonPrimary)
            ColorPreview("buttonPrimaryHover", v3Colors.buttonPrimaryHover)
            ColorPreview("buttonPrimaryPressed", v3Colors.buttonPrimaryPressed)
            ColorPreview("buttonSecondary", v3Colors.buttonSecondary)
            ColorPreview("buttonSecondaryHover", v3Colors.buttonSecondaryHover)
            ColorPreview("buttonSecondaryPressed", v3Colors.buttonSecondaryPressed)
            ColorPreview("background", v3Colors.background)
            ColorPreview("backgroundBrand", v3Colors.backgroundBrand)
            ColorPreview("border", v3Colors.border)
            ColorPreview("borderBrand", v3Colors.borderBrand)
        }
    }
}

@Composable
private fun ColorPreview(colorText: String, color: Color) {
    Row {
        Box(
            Modifier
                .size(40.dp)
                .background(color)
        )
        Text(
            text = colorText,
            style = v3Typography.bodyMedium,
            modifier = Modifier.padding(10.dp)
        )
    }
}
