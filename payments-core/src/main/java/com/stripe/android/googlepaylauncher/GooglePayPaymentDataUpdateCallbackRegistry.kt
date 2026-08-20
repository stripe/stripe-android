package com.stripe.android.googlepaylauncher

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object GooglePayPaymentDataUpdateCallbackRegistry {
    private val registeredCallbacks = mutableMapOf<String, GooglePayPaymentDataUpdateCallback>()

    @MainThread
    fun register(key: String, callback: GooglePayPaymentDataUpdateCallback) {
        registeredCallbacks[key] = callback
    }

    @MainThread
    fun deregister(key: String) {
        registeredCallbacks.remove(key)
    }

    fun get(key: String): GooglePayPaymentDataUpdateCallback? {
        return registeredCallbacks[key]
    }
}
