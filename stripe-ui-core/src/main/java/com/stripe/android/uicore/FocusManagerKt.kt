package com.stripe.android.uicore

import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import com.stripe.android.core.Logger

/**
 * Moves focus without throwing exceptions.
 *
 * Sometimes moveFocus will throw an IllegalArgumentException (https://github.com/stripe/stripe-android/issues/9300).
 * We should log + silently fail instead of crashing when this exception occurs.
 */
internal fun FocusManager.moveFocusSafely(focusDirection: FocusDirection): Boolean {
    try {
        return this.moveFocus(focusDirection)
    } catch (e: IllegalArgumentException) {
        Logger.getInstance(BuildConfig.DEBUG).warning("Skipping moving focus due to exception: $e")
        // This indicates that focus was not moved.
        return false
    }
}
