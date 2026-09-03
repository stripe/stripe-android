package com.stripe.android.paymentsheet.addresselement

import java.lang.ref.WeakReference
import java.util.UUID

internal object CheckoutShippingAddressUpdaterRegistry {
    interface Updater {
        suspend fun update(address: AddressDetails): Result<Unit>
    }

    private val updaters = mutableMapOf<String, WeakReference<Updater>>()

    @Synchronized
    fun register(updater: Updater): String {
        return UUID.randomUUID().toString().also { key ->
            updaters[key] = WeakReference(updater)
        }
    }

    @Synchronized
    fun register(key: String, updater: Updater) {
        updaters[key] = WeakReference(updater)
    }

    @Synchronized
    fun get(key: String?): Updater? {
        if (key == null) return null

        return updaters[key]?.get().also { updater ->
            if (updater == null) {
                updaters.remove(key)
            }
        }
    }

    @Synchronized
    fun remove(key: String?) {
        if (key != null) {
            updaters.remove(key)
        }
    }
}
