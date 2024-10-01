package com.stripe.android.financialconnections.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.compose.rememberNavController
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarHost
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.Theme
import com.stripe.android.uicore.image.StripeImageLoader

/**
 * Provides the entire context needed to render a preview.
 *
 * - Theme.
 * - CompositionLocalProviders.
 */
@Composable
internal fun FinancialConnectionsPreview(
    theme: Theme = Theme.default,
    testMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val navController = rememberNavController()
    FinancialConnectionsTheme(theme) {
        CompositionLocalProvider(
            LocalNavHostController provides navController,
            LocalTestMode provides testMode,
            LocalImageLoader provides StripeImageLoader(),
            LocalTopAppBarHost provides PreviewTopAppBarHost(),
            content = content
        )
    }
}

private class PreviewTopAppBarHost : TopAppBarHost {
    override fun updateTopAppBarElevation(isElevated: Boolean) {
        // Nothing to do here
    }
}
