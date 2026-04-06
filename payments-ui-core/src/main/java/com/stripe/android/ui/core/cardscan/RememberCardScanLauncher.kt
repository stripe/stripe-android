package com.stripe.android.ui.core.cardscan

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityOptionsCompat
import com.stripe.android.ui.core.cardscan.CardScanGoogleLauncher.Companion.rememberCardScanGoogleLauncher
import com.stripe.android.ui.core.cardscan.CardScanStripeLauncher.Companion.rememberCardScanStripeLauncher
import com.stripe.android.uicore.utils.AnimationConstants

@Composable
internal fun rememberCardScanLauncher(
    isStripeCardScanAllowed: Boolean = false,
    enableMlKitCardScan: Boolean = false,
    onResult: (CardScanResult) -> Unit,
    isStripeCardScanAvailable: IsStripeCardScanAvailable = DefaultIsStripeCardScanAvailable(),
): CardScanLauncher? {
    // Only create launcher if ActivityResultRegistry is available (e.g., not in screenshot tests)
    LocalActivityResultRegistryOwner.current ?: return null

    val eventsReporter = LocalCardScanEventsReporter.current

    return if (isStripeCardScanAllowed && isStripeCardScanAvailable()) {
        rememberCardScanStripeLauncher(
            eventsReporter = eventsReporter,
            enableMlKitCardScan = enableMlKitCardScan,
            onResult = onResult,
        )
    } else {
        val context = LocalContext.current
        val options = rememberActivityOptionsCompat()
        rememberCardScanGoogleLauncher(
            context = context,
            eventsReporter = eventsReporter,
            options = options,
            onResult = onResult,
        )
    }
}

@Composable
private fun rememberActivityOptionsCompat(): ActivityOptionsCompat {
    val context = LocalContext.current
    return remember(context) {
        ActivityOptionsCompat.makeCustomAnimation(
            context,
            AnimationConstants.FADE_IN,
            AnimationConstants.FADE_OUT,
        )
    }
}
