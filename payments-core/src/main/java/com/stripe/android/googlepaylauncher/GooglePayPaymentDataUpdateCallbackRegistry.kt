package com.stripe.android.googlepaylauncher

import androidx.annotation.RestrictTo
import java.util.Collections.synchronizedMap

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object GooglePayPaymentDataUpdateCallbackRegistry {
    private val registeredCallbacks = synchronizedMap(mutableMapOf<String, GooglePayPaymentDataUpdateCallback>())

    fun register(key: String, callback: GooglePayPaymentDataUpdateCallback) {
        registeredCallbacks[key] = callback
    }

    fun deregister(key: String) {
        registeredCallbacks.remove(key)
    }

    fun get(key: String): GooglePayPaymentDataUpdateCallback? {
        return registeredCallbacks[key]
    }
}
