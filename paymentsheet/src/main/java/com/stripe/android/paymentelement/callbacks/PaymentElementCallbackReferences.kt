package com.stripe.android.paymentelement.callbacks

import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

internal object PaymentElementCallbackReferences {
    private val instanceCallbackMap = mutableMapOf<String, Entry>()

    operator fun get(key: String): PaymentElementCallbacks? {
        /*
         * If an instance does not have callbacks assigned, we fallback to the default behavior and fetch the
         * first callbacks assigned to a Payment Element instance.
         */
        return (instanceCallbackMap[key] ?: instanceCallbackMap.values.firstOrNull())?.callbacks
    }

    /**
     * Registers callbacks until [owner] is destroyed. Replacing a registration transfers ownership of
     * the entry, so updates and cleanup from the previous registration cannot affect its replacement.
     * An already destroyed owner receives an inactive registration without changing existing callbacks.
     */
    @MainThread
    fun register(key: String, owner: LifecycleOwner, callbacks: PaymentElementCallbacks): Registration {
        val token = Any()
        val registration = Registration(key, token)
        val lifecycle = owner.lifecycle
        if (lifecycle.currentState != Lifecycle.State.DESTROYED) {
            instanceCallbackMap[key] = Entry(token, callbacks)
            lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onDestroy(owner: LifecycleOwner) {
                        if (instanceCallbackMap[key]?.token === token) {
                            instanceCallbackMap.remove(key)
                        }
                        owner.lifecycle.removeObserver(this)
                    }
                }
            )
        }
        return registration
    }

    @VisibleForTesting
    fun clear() {
        instanceCallbackMap.clear()
    }

    class Registration internal constructor(
        private val key: String,
        private val token: Any,
    ) {
        /** Updates only this registration's callbacks; inactive registrations cannot reclaim ownership. */
        @MainThread
        fun update(callbacks: PaymentElementCallbacks) {
            instanceCallbackMap[key]?.takeIf { it.token === token }?.callbacks = callbacks
        }
    }

    private class Entry(
        val token: Any,
        var callbacks: PaymentElementCallbacks,
    )
}
