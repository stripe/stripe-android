package com.stripe.android.paymentelement.callbacks

object PaymentElementCallbackStorage {
    private val instanceCallbackMap = mutableMapOf<String, PaymentElementCallbacks>()

    operator fun get(key: String): PaymentElementCallbacks? {
        return instanceCallbackMap[key]
    }

    operator fun set(key: String, callbacks: PaymentElementCallbacks) {
        instanceCallbackMap[key] = callbacks
    }

    fun remove(key: String) {
        instanceCallbackMap.remove(key)
    }
}
