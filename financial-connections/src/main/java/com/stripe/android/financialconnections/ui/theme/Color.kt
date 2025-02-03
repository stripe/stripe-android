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

// Neutral
internal val Neutral0 = Color(0xFFFFFFFF)
internal val Neutral25 = Color(0xFFF5F6F8)
internal val Neutral50 = Color(0xFFF6F8FA)
internal val Neutral100 = Color(0xFFD8DEE4)
internal val Neutral200 = Color(0xFFC0C8D2)
internal val Neutral600 = Color(0xFF596171)
internal val Neutral700 = Color(0xFF474E5A)
internal val Neutral800 = Color(0xFF353A44)
internal val Neutral900 = Color(0xff21252C)

// Attention
internal val Attention50 = Color(0xFFFEF9DA)
internal val Attention300 = Color(0xFFF7870F)

// Feedback
internal val FeedbackCritical600 = Color(0xFFC0123C)

// Brand
internal val Brand25 = Color(0xFFF7F5FD)
internal val Brand400 = Color(0xff8D7FFA)
internal val Brand500 = Color(0xFF675DFF)
internal val Brand600 = Color(0xFF533AFD)

// Link
internal val LinkGreen50 = Color(0xFFE6FFED)
internal val LinkGreen200 = Color(0xFF00D66F)
internal val LinkGreen500 = Color(0xFF008545)
internal val LinkGreen900 = Color(0xFF011E0F)

@Immutable
internal data class FinancialConnectionsColors(
    val background: Color,
    val backgroundSecondary: Color,
    val backgroundHighlighted: Color,
    val textDefault: Color,
    val textSubdued: Color,
    val textCritical: Color,
    val icon: Color,
    val borderNeutral: Color,
    val spinnerNeutral: Color,
    val warningLight: Color,
    val warning: Color,
    val primary: Color,
    val primaryAccent: Color,
    val textAction: Color,
    val textFieldFocused: Color,
    val logo: Color,
    val iconTint: Color,
    val iconBackground: Color,
    val spinner: Color,
    val border: Color,
)

@Preview(group = "Components", name = "Colors")
@Composable
internal fun ColorsPreview() {
    FinancialConnectionsPreview {
        Column(
            modifier = Modifier.background(Color.White)
        ) {
            ColorPreview("background", colors.background)
            ColorPreview("backgroundSecondary", colors.backgroundSecondary)
            ColorPreview("backgroundHighlighted", colors.backgroundHighlighted)
            ColorPreview("textDefault", colors.textDefault)
            ColorPreview("textSubdued", colors.textSubdued)
            ColorPreview("textCritical", colors.textCritical)
            ColorPreview("icon", colors.icon)
            ColorPreview("borderNeutral", colors.borderNeutral)
            ColorPreview("spinnerNeutral", colors.spinnerNeutral)
            ColorPreview("warningLight", colors.warningLight)
            ColorPreview("warning", colors.warning)
            ColorPreview("primary", colors.primary)
            ColorPreview("primaryAccent", colors.primaryAccent)
            ColorPreview("textAction", colors.textAction)
            ColorPreview("textFieldFocused", colors.textFieldFocused)
            ColorPreview("logo", colors.logo)
            ColorPreview("iconTint", colors.iconTint)
            ColorPreview("iconBackground", colors.iconBackground)
            ColorPreview("spinner", colors.spinner)
            ColorPreview("border", colors.border)
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
