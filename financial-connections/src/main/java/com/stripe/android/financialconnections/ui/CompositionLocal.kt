package com.stripe.android.financialconnections.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.presentation.TopAppBarHost
import com.stripe.android.financialconnections.ui.components.TopAppBarState
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
    testMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val navController = rememberNavController()

    val topAppBarHost = remember {
        object : TopAppBarHost {
            override val defaultTopAppBarState: TopAppBarState = TopAppBarState(
                pane = FinancialConnectionsSessionManifest.Pane.CONSENT,
                hideStripeLogo = reducedBrandingOverride,
                testMode = testMode,
                allowBackNavigation = true,
            )

            override fun handleTopAppBarStateChanged(topAppBarState: TopAppBarState) = Unit
            override fun updateTopAppBarElevation(isElevated: Boolean) = Unit
        }
    }

    FinancialConnectionsTheme {
        CompositionLocalProvider(
            LocalNavHostController provides navController,
            LocalTestMode provides testMode,
            LocalReducedBranding provides reducedBrandingOverride,
            LocalImageLoader provides StripeImageLoader(
                context = LocalContext.current,
                logger = Logger.noop(),
                memoryCache = null,
                networkImageDecoder = NetworkImageDecoder(),
                diskCache = null
            ),
            LocalTopAppBarHost provides topAppBarHost,
            content = content
        )
    }
}
