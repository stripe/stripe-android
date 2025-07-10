package com.stripe.android.link.ui.wallet

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.OTPElementColors

internal data class LinkOtpSectionTheme(
    /**
     * Background color for focused digit input boxes
     */
    val focusedBackground: Color,

    /**
     * Border color for digit input boxes
     */
    val borderColor: Color,

    /**
     * Text color for the digits
     */
    val textColor: Color,

    /**
     * Focus ring color around active digit box
     */
    val selectedBorderColor: Color,

    /**
     * Background color when the field is disabled
     */
    val disabledBackground: Color,

    /**
     * Corner radius shape for input boxes
     */
    val cornerShape: Shape,

    /**
     * Typography style for the title text with scaling applied
     */
    val titleTextStyle: TextStyle,

    /**
     * Normal background color for unfocused inputs
     */
    val normalBackground: Color
) {
    /**
     * Create OTPElementColors configuration with proper appearance theming
     */
    fun createOtpColors(isProcessing: Boolean): OTPElementColors {
        return OTPElementColors(
            selectedBorder = selectedBorderColor,
            unselectedBorder = borderColor,
            placeholder = textColor,
            selectedBackground = focusedBackground,
            background = if (isProcessing) disabledBackground else normalBackground,
        )
    }
}

/**
 * Creates a LinkOtpSectionTheme from PaymentSheet.Appearance configuration.
 *
 * @param appearance The PaymentSheet appearance configuration, or null for defaults
 * @param isDark Whether the current system is in dark mode
 * @return Configured theme with all computed values
 */
@Composable
internal fun createLinkOtpSectionTheme(
    appearance: PaymentSheet.Appearance?,
    isDark: Boolean = isSystemInDarkTheme()
): LinkOtpSectionTheme {
    val colors = appearance?.getColors(isDark)
    val cornerShape = appearance?.shapes?.cornerRadiusDp?.let { RoundedCornerShape(it.dp) }
    val titleStyle = LinkTheme.typography.body
    return LinkOtpSectionTheme(
        focusedBackground = extractColor(colors?.component, LinkTheme.colors.surfacePrimary),
        borderColor = extractColor(colors?.componentBorder, LinkTheme.colors.borderDefault),
        textColor = extractColor(colors?.onComponent, LinkTheme.colors.textPrimary),
        selectedBorderColor = extractColor(colors?.primary, LinkTheme.colors.borderSelected),
        disabledBackground = extractColor(colors?.component, LinkTheme.colors.surfaceSecondary)
            .copy(alpha = 0.6f),
        cornerShape = cornerShape ?: LinkTheme.shapes.default,
        titleTextStyle = if (appearance != null) {
            titleStyle.copy(fontSize = titleStyle.fontSize * appearance.typography.sizeScaleFactor)
        } else {
            titleStyle
        },
        normalBackground = LinkTheme.colors.surfaceSecondary
    )
}

private fun extractColor(colorValue: Int?, fallback: Color): Color {
    return colorValue?.let { Color(it) } ?: fallback
}
