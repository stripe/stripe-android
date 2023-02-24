package com.stripe.android.financialconnections.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.uicore.image.NetworkImageDecoder
import com.stripe.android.uicore.image.StripeImageLoader

/**
 * Provides the entire context needed to render a preview.
 *
 * - Theme.
 * - CompositionLocalProviders.
 */
@Composable
internal fun FinancialConnectionsPreview(
    reducedBrandingOverride: Boolean = false,
    content: @Composable () -> Unit
) {
    val navController = rememberNavController()
    FinancialConnectionsTheme {
        CompositionLocalProvider(
            LocalNavHostController provides navController,
            LocalReducedBranding provides reducedBrandingOverride,
            LocalImageLoader provides StripeImageLoader(
                context = LocalContext.current,
                logger = Logger.noop(),
                memoryCache = null,
                networkImageDecoder = NetworkImageDecoder(),
                diskCache = null
            ),
            content = content
        )
    }
}
