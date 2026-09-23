package com.stripe.android.paymentelement.callbacks

import androidx.annotation.VisibleForTesting

internal object PaymentElementCallbackReferences {
    private val instanceCallbackMap = mutableMapOf<CallbacksKey, PaymentElementCallbacks>()

    operator fun get(key: CallbacksKey): PaymentElementCallbacks? {
        return instanceCallbackMap[key]
    }

    operator fun set(key: CallbacksKey, callbacks: PaymentElementCallbacks) {
        instanceCallbackMap[key] = callbacks
    }

    fun remove(key: CallbacksKey) {
        instanceCallbackMap.remove(key)
    }

    @VisibleForTesting
    fun clear() {
        instanceCallbackMap.clear()
    }
}
