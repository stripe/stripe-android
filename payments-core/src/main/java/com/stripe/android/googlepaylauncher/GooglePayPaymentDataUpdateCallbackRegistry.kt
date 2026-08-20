package com.stripe.android.googlepaylauncher

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object GooglePayPaymentDataUpdateCallbackRegistry {
    private val registeredCallbacks: MutableMap<String, GooglePayPaymentDataUpdateCallback> = mutableMapOf()
    private val lock = Any()

    fun register(key: String, callback: GooglePayPaymentDataUpdateCallback) {
        synchronized(lock) {
            registeredCallbacks[key] = callback
        }
    }

    fun deregister(key: String) {
        synchronized(lock) {
            registeredCallbacks.remove(key)
        }
    }

    fun get(key: String): GooglePayPaymentDataUpdateCallback? {
        synchronized(lock) {
            return registeredCallbacks[key]
        }
    }
}
