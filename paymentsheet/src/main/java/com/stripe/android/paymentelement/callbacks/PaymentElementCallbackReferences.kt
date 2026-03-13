package com.stripe.android.paymentelement.callbacks

import androidx.annotation.VisibleForTesting

internal object PaymentElementCallbackReferences {
    private val instanceCallbackMap = mutableMapOf<String, PaymentElementCallbacks>()

    operator fun get(key: String): PaymentElementCallbacks? {
        /*
         * If an instance does not have callbacks assigned, we fallback to the default behavior and fetch the
         * first callbacks assigned to a Payment Element instance.
         */
        val r = instanceCallbackMap[key]
        val fallback = instanceCallbackMap.values.firstOrNull()
//        println("YEET instanceCallbackMap[key]: $r")
////        println("YEET key: $key")
////        println("YEET fallback: $fallback")
        return r ?: fallback
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
