package com.stripe.android.paymentelement.callbacks

import androidx.annotation.VisibleForTesting

internal object PaymentElementCallbackReferences {
    private val instanceCallbackMap = mutableMapOf<String, PaymentElementCallbacks>()

    operator fun get(key: String): PaymentElementCallbacks? {
        return instanceCallbackMap[key] ?: instanceCallbackMap.values.firstOrNull()
    }

    operator fun set(key: String, callbacks: PaymentElementCallbacks) {
        instanceCallbackMap[key] = callbacks
    }

    fun remove(key: String) {
        instanceCallbackMap.remove(key)
    }

    @VisibleForTesting
    fun clear() {
        instanceCallbackMap.clear()
    }
}
