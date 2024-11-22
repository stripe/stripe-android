package com.stripe.android.financialconnections

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForDataContract
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForDataLauncher
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForTokenContract
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForTokenLauncher

/**
 * Register a request to launch an instance of [FinancialConnectionsSheet]
 * for a [FinancialConnectionsSheetResult].
 *
 * This API uses Compose specific API [rememberLauncherForActivityResult] to register a
 * [androidx.activity.result.ActivityResultLauncher] into the current activity,
 * so it should be called as part of Compose initialization path.
 *
 * The created FinancialConnectionsSheet is remembered across recompositions.
 * Recomposition will always return the value produced by composition.
 */
@Composable
fun rememberFinancialConnectionsSheet(
    callback: (FinancialConnectionsSheetResult) -> Unit
): FinancialConnectionsSheet {
    val activityResultLauncher = rememberLauncherForActivityResult(
        FinancialConnectionsSheetForDataContract()
    ) { callback(it.toPublicResult()) }
    return remember {
        FinancialConnectionsSheet(
            FinancialConnectionsSheetForDataLauncher(
                activityResultLauncher
            )
        )
    }
}

/**
 * Register a request to launch an instance of [FinancialConnectionsSheet]
 * for a [FinancialConnectionsSheetForTokenResult].
 *
 * This API uses Compose specific API [rememberLauncherForActivityResult] to register a
 * [androidx.activity.result.ActivityResultLauncher] into the current activity,
 * so it should be called as part of Compose initialization path.
 *
 * The created FinancialConnectionsSheet is remembered across recompositions.
 * Recomposition will always return the value produced by composition.
 */
@Composable
fun rememberFinancialConnectionsSheetForToken(
    callback: (FinancialConnectionsSheetForTokenResult) -> Unit
): FinancialConnectionsSheet {
    val activityResultLauncher = rememberLauncherForActivityResult(
        FinancialConnectionsSheetForTokenContract()
    ) { callback(it) }
    return remember {
        FinancialConnectionsSheet(
            FinancialConnectionsSheetForTokenLauncher(
                activityResultLauncher
            )
        )
    }
}
