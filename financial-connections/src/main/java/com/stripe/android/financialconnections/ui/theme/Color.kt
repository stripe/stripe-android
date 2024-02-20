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
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography

internal val Neutral0 = Color(0xffFFFFFF)
internal val Neutral50 = Color(0xffF5F6F8)
internal val Neutral900 = Color(0xff21252C)

internal val Brand50 = Color(0xffF7F5FD)
internal val Brand400 = Color(0xff8D7FFA)

internal object LinkColors {
    val Brand200 = Color(0xffA6FBDD)
    val Brand600 = Color(0xff1AC59B)
}

@Immutable
internal data class FinancialConnectionsColors(
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
            ColorPreview("textDefault", colors.textDefault)
            ColorPreview("textSubdued", colors.textSubdued)
            ColorPreview("textDisabled", colors.textDisabled)
            ColorPreview("textWhite", colors.textWhite)
            ColorPreview("textBrand", colors.textBrand)
            ColorPreview("textCritical", colors.textCritical)
            ColorPreview("iconDefault", colors.iconDefault)
            ColorPreview("iconSubdued", colors.iconSubdued)
            ColorPreview("iconWhite", colors.iconWhite)
            ColorPreview("iconBrand", colors.iconBrand)
            ColorPreview("buttonPrimary", colors.buttonPrimary)
            ColorPreview("buttonPrimaryHover", colors.buttonPrimaryHover)
            ColorPreview("buttonPrimaryPressed", colors.buttonPrimaryPressed)
            ColorPreview("buttonSecondary", colors.buttonSecondary)
            ColorPreview("buttonSecondaryHover", colors.buttonSecondaryHover)
            ColorPreview("buttonSecondaryPressed", colors.buttonSecondaryPressed)
            ColorPreview("background", colors.background)
            ColorPreview("backgroundBrand", colors.backgroundBrand)
            ColorPreview("border", colors.border)
            ColorPreview("borderBrand", colors.borderBrand)
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
            style = typography.bodyMedium,
            modifier = Modifier.padding(10.dp)
        )
    }
}
