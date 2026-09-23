package com.stripe.android.paymentelement.callbacks

import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import java.util.UUID

internal class LifecyclePaymentElementCallbackReferences @MainThread constructor(
    private val lifecycle: Lifecycle,
) {
    private val suffix = UUID.randomUUID().toString()
    private val referenceKeys = mutableSetOf<String>()

    init {
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    referenceKeys.forEach(PaymentElementCallbackReferences::remove)
                    referenceKeys.clear()
                    lifecycle.removeObserver(this)
                }
            }
        )
    }

    @MainThread
    operator fun set(key: String, callbacks: PaymentElementCallbacks) {
        if (lifecycle.currentState != Lifecycle.State.DESTROYED) {
            val resolvedKey = "${key}_$suffix"
            referenceKeys.add(resolvedKey)
            PaymentElementCallbackReferences.set(key = key, referenceKey = resolvedKey, callbacks = callbacks)
        }
    }
}
