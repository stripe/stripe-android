package com.stripe.android.paymentelement.callbacks

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

    fun clear() {
        instanceCallbackMap.clear()
    }
}
