package com.stripe.android.financialconnections.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.StripeThemeDefaults

@Composable
internal fun StripeThemeForConnections(
    componentBackground: Color? = null,
    content: @Composable () -> Unit
) {
    // Financial Connections does not currently support dark mode.
    val stripeDefaultColors = StripeThemeDefaults.colors(isDark = false)
    // Read the composable theme values unconditionally. Calling them inside the `?:` fallback
    // only adds a composition group when `componentBackground` is null, so toggling it (e.g. on
    // focus) shifts the groups and recreates the whole StripeTheme subtree, dropping focus.
    val isLink = FinancialConnectionsTheme.theme.isLink
    val themeColors = colors
    val defaultComponent = if (isLink) themeColors.iconBackground else themeColors.background
    StripeTheme(
        colors = stripeDefaultColors.copy(
            component = componentBackground ?: defaultComponent,
            onComponent = themeColors.textDefault,
            componentBorder = themeColors.borderNeutral,
            placeholderText = themeColors.textSubdued,
            textCursor = themeColors.textDefault,
            materialColors = stripeDefaultColors.materialColors.copy(
                primary = themeColors.border,
                error = themeColors.textCritical,
            )
        ),
        shapes = StripeThemeDefaults.shapes.copy(
            cornerRadius = 12f
        ),
        typography = StripeThemeDefaults.typography,
        sectionSpacing = StripeThemeDefaults.sectionSpacing,
        sectionStyle = StripeThemeDefaults.sectionStyle,
        textFieldInsets = StripeThemeDefaults.textFieldInsets,
        iconStyle = StripeThemeDefaults.iconStyle,
    ) {
        content()
    }
}
