package com.stripe.android.googlepaylauncher

import androidx.annotation.MainThread
import com.stripe.android.core.Identifiable
import kotlinx.coroutines.CoroutineScope

internal object GooglePayPaymentDataUpdateCallbackRegistry {
    private var selection: Selection? = null
    private val registeredCallbacks = mutableMapOf<Identifiable, GooglePayPaymentDataUpdateCallback>()

    @MainThread
    fun register(key: Identifiable, callback: GooglePayPaymentDataUpdateCallback) {
        registeredCallbacks[key] = callback
    }

    @MainThread
    fun deregister(key: Identifiable) {
        registeredCallbacks.remove(key)
    }

    fun select(key: Identifiable, workScope: CoroutineScope) {
        selection = registeredCallbacks[key]?.let {
            Selection(callback = it, workScope = workScope)
        }
    }

    fun deselect() {
        selection = null
    }

    fun get(): Selection? {
        return selection
    }

    class Selection(
        val callback: GooglePayPaymentDataUpdateCallback,
        val workScope: CoroutineScope,
    )
}
