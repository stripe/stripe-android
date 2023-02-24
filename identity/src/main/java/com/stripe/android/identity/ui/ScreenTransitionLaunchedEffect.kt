package com.stripe.android.identity.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.viewmodel.IdentityViewModel

/**
 * LaunchedEffect to track screen transition.
 */
@Composable
internal fun ScreenTransitionLaunchedEffect(
    identityViewModel: IdentityViewModel,
    screenName: String,
    scanType: IdentityScanState.ScanType? = null
) {
    LaunchedEffect(Unit) {
        // Tracks screen presented
        identityViewModel.trackScreenPresented(scanType, screenName)

        // Tracks screen transition finish
        identityViewModel.trackScreenTransitionFinish(screenName)
    }
}
