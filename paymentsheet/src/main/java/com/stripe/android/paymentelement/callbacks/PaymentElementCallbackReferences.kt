package com.stripe.android.paymentelement.callbacks

import androidx.annotation.VisibleForTesting
import com.stripe.android.paymentsheet.PaymentSheet.Identifiable

internal object PaymentElementCallbackReferences {
    private val instanceCallbackMap = mutableMapOf<Identifiable, PaymentElementCallbacks>()

    operator fun get(key: Identifiable): PaymentElementCallbacks? {
        /*
         * If an instance does not have callbacks assigned, we fallback to the default behavior and fetch the
         * first callbacks assigned to a Payment Element instance.
         */
        return instanceCallbackMap[key] ?: instanceCallbackMap.values.firstOrNull()
    }

    operator fun set(key: Identifiable, callbacks: PaymentElementCallbacks) {
        instanceCallbackMap[key] = callbacks
    }

    fun remove(key: Identifiable) {
        instanceCallbackMap.remove(key)
    }

    @VisibleForTesting
    fun clear() {
        instanceCallbackMap.clear()
    }
}
