package com.stripe.android.checkout

import androidx.lifecycle.LifecycleOwner

internal fun invalidHostStateError(
    lifecycleOwner: LifecycleOwner,
    cause: IllegalStateException? = null,
): IllegalStateException {
    val message = "The host activity is not in a valid state (${lifecycleOwner.lifecycle.currentState})."
    return IllegalStateException(message, cause)
}
