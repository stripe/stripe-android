package com.stripe.financialconnections.compose

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import com.stripe.android.connections.FinancialConnectionsSheetForTokenResult
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetContract
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.financialconnections.launcher.DefaultFinancialConnectionsSheetLauncher
import com.stripe.android.financialconnections.launcher.DefaultFinancialConnectionsSheetLauncher.Companion.toExposedResult as toExposedResultForData
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForTokenLauncher
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForTokenLauncher.Companion.toExposedResult as toExposedResultForToken

/**
 * Create a [FinancialConnectionsSheet] used for Jetpack Compose.
 *
 * This API uses Compose specific API [rememberLauncherForActivityResult] to register a
 * [androidx.activity.result.ActivityResultLauncher] into the current activity,
 * so it should be called as part of Compose initialization path.
 */
@Composable
@Suppress("UnusedPrivateMember")
fun FinancialConnectionsSheet.Companion.createComposable(
    callback: (FinancialConnectionsSheetResult) -> Unit
): FinancialConnectionsSheet = FinancialConnectionsSheet(
    DefaultFinancialConnectionsSheetLauncher(
        rememberLauncherForActivityResult(
            FinancialConnectionsSheetContract()
        ) {
            callback(it.toExposedResultForData())
        }
    )
)

/**
 * Create a [FinancialConnectionsSheet] used for Jetpack Compose.
 *
 * This API uses Compose specific API [rememberLauncherForActivityResult] to register a
 * [androidx.activity.result.ActivityResultLauncher] into the current activity,
 * so it should be called as part of Compose initialization path.
 */
@Composable
@Suppress("UnusedPrivateMember")
private fun FinancialConnectionsSheet.Companion.createComposableForToken(
    callback: (FinancialConnectionsSheetForTokenResult) -> Unit
): FinancialConnectionsSheet = FinancialConnectionsSheet(
    FinancialConnectionsSheetForTokenLauncher(
        rememberLauncherForActivityResult(
            FinancialConnectionsSheetContract()
        ) {
            callback(it.toExposedResultForToken())
        }
    )
)
