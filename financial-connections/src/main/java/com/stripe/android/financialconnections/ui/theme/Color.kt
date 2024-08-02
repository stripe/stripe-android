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
internal val Neutral25 = Color(0xffF5F6F8)
internal val Neutral50 = Color(0xffF6F8FA)
internal val Neutral100 = Color(0xffD8DEE4)
internal val Neutral300 = Color(0xffA3ACBA)
internal val Neutral600 = Color(0xff596171)
internal val Neutral700 = Color(0xff474E5A)
internal val Neutral800 = Color(0xff353A44)
internal val Neutral900 = Color(0xff21252C)

internal val Brand50 = Color(0xffF7F5FD)
internal val Brand400 = Color(0xff8D7FFA)
internal val Brand500 = Color(0xff675dff)
internal val Brand600 = Color(0xff533AFD)

internal val Critical500 = Color(0xffDF1B41)

internal val Attention50 = Color(0xffFEF9DA)
internal val Attention300 = Color(0xffF7870F)

internal val LinkGreen50 = Color(0xffE6FFED)
internal val LinkGreen200 = Color(0xff00D66F)
internal val LinkGreen500 = Color(0xff008545)
internal val LinkGreen900 = Color(0xff011E0F)

@Immutable
internal data class FinancialConnectionsColors(
    val textDefault: Color,
    val textSubdued: Color,
    val textDisabled: Color,
    val textWhite: Color,
    val textBrand: Color,
    val textCritical: Color,
    val iconDefault: Color,
    val iconWhite: Color,
    val iconBrand: Color,
    val iconCaution: Color,
    val iconBackground: Color,
    val buttonPrimary: Color,
    val buttonSecondary: Color,
    val backgroundSurface: Color,
    val background: Color,
    val backgroundOffset: Color,
    val backgroundBrand: Color,
    val backgroundCaution: Color,
    val border: Color,
    val borderBrand: Color,
    val contentOnBrand: Color,
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
            ColorPreview("iconWhite", colors.iconWhite)
            ColorPreview("iconBrand", colors.iconBrand)
            ColorPreview("buttonPrimary", colors.buttonPrimary)
            ColorPreview("buttonSecondary", colors.buttonSecondary)
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
