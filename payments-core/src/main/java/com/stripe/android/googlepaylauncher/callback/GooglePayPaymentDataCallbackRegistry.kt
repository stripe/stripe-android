package com.stripe.android.googlepaylauncher.callback

import androidx.annotation.RestrictTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object GooglePayPaymentDataCallbackRegistry {
    val workScope = CoroutineScope(Dispatchers.IO)

    @Volatile
    private var selectedCallbackKey: String? = null

    @Volatile
    private var pendingPaymentChangeCompletion: Pair<GooglePayIntermediatePaymentData, (GooglePayPaymentDataRequestUpdate) -> Unit>? = null

    private val registeredCallbacks: MutableMap<String, GooglePayPaymentDataChangedCallback> = mutableMapOf()
    private val lock = Any()

    fun register(key: String, callback: GooglePayPaymentDataChangedCallback) {
        synchronized(lock) {
            registeredCallbacks[key] = callback
        }
    }

    fun deregister(key: String) {
        synchronized(lock) {
            registeredCallbacks.remove(key)
        }
    }

    fun handle(
        intermediatePaymentData: GooglePayIntermediatePaymentData,
        onComplete: (GooglePayPaymentDataRequestUpdate) -> Unit,
    ) {
        val callback = selectedCallback()
        if (callback != null) {
            workScope.launch {
                onComplete(callback.onPaymentDataChanged(intermediatePaymentData))
            }
            return
        }

        if (selectedCallbackKey != null) {
            onComplete(notRegisteredUpdate())
            return
        }

        pendingPaymentChangeCompletion = intermediatePaymentData to onComplete
    }

    fun select(key: String) {
        synchronized(lock) {
            selectedCallbackKey = key
        }

        pendingPaymentChangeCompletion?.let { (intermediatePaymentData, onComplete) ->
            pendingPaymentChangeCompletion = null
            val callback = synchronized(lock) {
                registeredCallbacks[key]
            }
            if (callback != null) {
                workScope.launch {
                    onComplete(callback.onPaymentDataChanged(intermediatePaymentData))
                }
            } else {
                onComplete(notRegisteredUpdate())
            }
        }
    }

    internal fun notRegisteredUpdate(): GooglePayPaymentDataRequestUpdate {
        return GooglePayPaymentDataRequestUpdate(
            error = GooglePayPaymentDataError(
                reason = GooglePayPaymentDataError.Reason.OTHER_ERROR,
                message = NOT_REGISTERED_ERROR_MESSAGE,
                intent = GooglePayPaymentDataError.Intent.SHIPPING_ADDRESS,
            ),
        )
    }

    internal const val NOT_REGISTERED_ERROR_MESSAGE = "Payment data callback is not registered."

    private fun selectedCallback(): GooglePayPaymentDataChangedCallback? {
        val key = selectedCallbackKey ?: return null
        return synchronized(lock) {
            registeredCallbacks[key]
        }
    }

    fun deselect() {
        synchronized(lock) {
            selectedCallbackKey = null
        }
    }
}
