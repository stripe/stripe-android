package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity

/**
 * Reveals the primary button when completing a form enables it while the IME is visible.
 *
 * @param resetKey identifies the form or screen whose enablement transition is being observed.
 * When it changes, the current [isEnabled] value becomes the new baseline instead of carrying
 * enablement state over from the previous form or screen.
 */
@Composable
internal fun RevealPrimaryButtonWhenEnabled(
    isEnabled: Boolean,
    bringIntoViewRequester: BringIntoViewRequester,
    resetKey: Any?,
) {
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    var wasEnabled by remember(resetKey) {
        mutableStateOf(isEnabled)
    }

    LaunchedEffect(isEnabled) {
        if (
            shouldRevealWhenEnabled(
                wasEnabled = wasEnabled,
                isEnabled = isEnabled,
                isImeVisible = isImeVisible,
            )
        ) {
            bringIntoViewRequester.bringIntoView()
        }
        wasEnabled = isEnabled
    }
}

internal fun shouldRevealWhenEnabled(
    wasEnabled: Boolean,
    isEnabled: Boolean,
    isImeVisible: Boolean,
): Boolean {
    return !wasEnabled && isEnabled && isImeVisible
}
