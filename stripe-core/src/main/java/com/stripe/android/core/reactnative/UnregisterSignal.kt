package com.stripe.android.core.reactnative

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ReactNativeSdkInternal
class UnregisterSignal {
    private var listener: (() -> Unit)? = null

    fun addListener(listener: () -> Unit) {
        this.listener = listener
    }

    fun unregister() {
        listener?.invoke()
        listener = null
    }
}
