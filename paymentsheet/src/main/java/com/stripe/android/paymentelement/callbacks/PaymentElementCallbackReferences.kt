package com.stripe.android.paymentelement.callbacks

import androidx.annotation.VisibleForTesting

internal object PaymentElementCallbackReferences {
    private val instanceCallbackMap = mutableMapOf<String, PaymentElementCallbacks>()

    @Synchronized
    operator fun get(key: String): PaymentElementCallbacks? {
        /*
         * If an instance does not have callbacks assigned, we fallback to the default behavior and fetch the
         * first callbacks assigned to a Payment Element instance.
         */
        return instanceCallbackMap[key]
            ?.takeIf { it.hasCallbacksBesidesShippingAddressUpdater() }
            ?: instanceCallbackMap.values.firstOrNull {
                it.hasCallbacksBesidesShippingAddressUpdater()
            }
    }

    @Synchronized
    operator fun set(key: String, callbacks: PaymentElementCallbacks) {
        val shippingAddressUpdater = instanceCallbackMap[key]?.shippingAddressUpdater
        instanceCallbackMap[key] = if (shippingAddressUpdater == null) {
            callbacks
        } else {
            callbacks.withShippingAddressUpdater(shippingAddressUpdater)
        }
    }

    @Synchronized
    fun remove(key: String) {
        val shippingAddressUpdater = instanceCallbackMap[key]?.shippingAddressUpdater
        if (shippingAddressUpdater == null) {
            instanceCallbackMap.remove(key)
        } else {
            instanceCallbackMap[key] = EMPTY_CALLBACKS.withShippingAddressUpdater(shippingAddressUpdater)
        }
    }

    @Synchronized
    fun registerShippingAddressUpdater(
        key: String,
        updater: ShippingAddressUpdater,
    ) {
        val callbacks = instanceCallbackMap[key] ?: EMPTY_CALLBACKS
        instanceCallbackMap[key] = callbacks.withShippingAddressUpdater(updater)
    }

    @Synchronized
    fun getShippingAddressUpdater(key: String): ShippingAddressUpdater? {
        return instanceCallbackMap[key]?.shippingAddressUpdater
    }

    @Synchronized
    fun unregisterShippingAddressUpdater(
        key: String,
        updater: ShippingAddressUpdater,
    ) {
        val callbacks = instanceCallbackMap[key] ?: return
        if (callbacks.shippingAddressUpdater !== updater) return

        val callbacksWithoutUpdater = callbacks.withShippingAddressUpdater(null)
        if (callbacksWithoutUpdater == EMPTY_CALLBACKS) {
            instanceCallbackMap.remove(key)
        } else {
            instanceCallbackMap[key] = callbacksWithoutUpdater
        }
    }

    @VisibleForTesting
    @Synchronized
    fun clear() {
        instanceCallbackMap.clear()
    }

    private val EMPTY_CALLBACKS = PaymentElementCallbacks.Builder().build()
}
