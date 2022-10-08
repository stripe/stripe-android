package com.stripe.android.financialconnections.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

internal val LocalNavHostController = staticCompositionLocalOf<NavHostController> {
    error("No NavHostController provided")
}

/**
 * Provides the entire context needed to render a preview.
 *
 * - Theme.
 * - CompositionLocalProviders.
 */
@Composable
internal fun FinancialConnectionsPreview(
    content: @Composable () -> Unit
) {
    val navController = rememberNavController()
    FinancialConnectionsTheme {
        CompositionLocalProvider(
            LocalNavHostController provides navController,
            content = content
        )
    }
}
