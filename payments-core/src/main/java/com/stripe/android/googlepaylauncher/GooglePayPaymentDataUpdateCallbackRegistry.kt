package com.stripe.android.googlepaylauncher

import androidx.annotation.MainThread
import com.stripe.android.core.ApiConfiguration
import kotlinx.coroutines.CoroutineScope

internal object GooglePayPaymentDataUpdateCallbackRegistry {
    private var selection: Selection? = null
    private val registeredCallbacks = mutableMapOf<String, GooglePayPaymentDataUpdateCallback>()

    @MainThread
    fun register(key: String, callback: GooglePayPaymentDataUpdateCallback) {
        registeredCallbacks[key] = callback
    }

    @MainThread
    fun deregister(key: String) {
        registeredCallbacks.remove(key)
    }

    fun select(key: String, workScope: CoroutineScope, apiConfiguration: ApiConfiguration.State) {
        selection = registeredCallbacks[key]?.let {
            Selection(callback = it, workScope = workScope, apiConfiguration = apiConfiguration)
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
        val apiConfiguration: ApiConfiguration.State,
    )
}
