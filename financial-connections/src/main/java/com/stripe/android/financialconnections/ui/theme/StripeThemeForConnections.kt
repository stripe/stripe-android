package com.stripe.android.financialconnections.ui.theme

import androidx.compose.runtime.Composable
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.StripeThemeDefaults

@Composable
internal fun StripeThemeForConnections(
    content: @Composable () -> Unit
) {
    // Financial Connections does not currently support dark mode.
    val stripeDefaultColors = StripeThemeDefaults.colors(isDark = false)
    StripeTheme(
        colors = stripeDefaultColors.copy(
            component = colors.background,
            onComponent = colors.textDefault,
            componentBorder = colors.borderNeutral,
            placeholderText = colors.textSubdued,
            textCursor = colors.textDefault,
            materialColors = stripeDefaultColors.materialColors.copy(
                primary = colors.border,
                error = colors.textCritical,
            )
        ),
        shapes = StripeThemeDefaults.shapes.copy(
            cornerRadius = 12f
        ),
        typography = StripeThemeDefaults.typography
    ) {
        content()
    }
}
