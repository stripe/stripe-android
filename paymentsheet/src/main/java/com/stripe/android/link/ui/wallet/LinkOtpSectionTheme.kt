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
    return LinkOtpSectionTheme(
        focusedBackground = colors?.component?.let { Color(it) } ?: LinkTheme.colors.surfacePrimary,
        borderColor = colors?.componentBorder?.let { Color(it) } ?: LinkTheme.colors.borderDefault,
        textColor = colors?.onComponent?.let { Color(it) } ?: LinkTheme.colors.textPrimary,
        selectedBorderColor = colors?.primary?.let { Color(it) } ?: LinkTheme.colors.borderSelected,
        disabledBackground = (colors?.component?.let { Color(it) } ?: LinkTheme.colors.surfaceSecondary)
            .copy(alpha = 0.6f),
        cornerShape = appearance?.shapes?.cornerRadiusDp?.let { RoundedCornerShape(it.dp) }
            ?: LinkTheme.shapes.default,
        titleTextStyle = run {
            val baseStyle = LinkTheme.typography.body
            if (appearance != null) {
                baseStyle.copy(fontSize = baseStyle.fontSize * appearance.typography.sizeScaleFactor)
            } else {
                baseStyle
            }
        },
        normalBackground = LinkTheme.colors.surfaceSecondary
    )
} 