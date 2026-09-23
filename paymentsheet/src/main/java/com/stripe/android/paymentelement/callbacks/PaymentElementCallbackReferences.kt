package com.stripe.android.paymentelement.callbacks

import androidx.annotation.VisibleForTesting

internal object PaymentElementCallbackReferences {
    private val instanceCallbackMap = mutableMapOf<String, CallbackReference>()

    operator fun get(key: String): PaymentElementCallbacks? {
        /*
         * Lifecycle scopes use distinct storage keys but share a stable lookup identifier. The most recently
         * created reference wins; updating an older reference does not change that order.
         *
         * If an instance does not have callbacks assigned, we fallback to the default behavior and fetch the
         * first callbacks assigned to a Payment Element instance.
         */
        val reference = instanceCallbackMap.values.lastOrNull { it.key == key }
            ?: instanceCallbackMap.values.firstOrNull()
        return reference?.callbacks
    }

    operator fun set(key: String, callbacks: PaymentElementCallbacks) {
        set(key = key, referenceKey = key, callbacks = callbacks)
    }

    fun set(key: String, referenceKey: String, callbacks: PaymentElementCallbacks) {
        instanceCallbackMap[referenceKey] = CallbackReference(key, callbacks)
    }

    fun remove(referenceKey: String) {
        instanceCallbackMap.remove(referenceKey)
    }

    @VisibleForTesting
    fun clear() {
        instanceCallbackMap.clear()
    }

    private class CallbackReference(
        val key: String,
        val callbacks: PaymentElementCallbacks,
    )
}
