package com.stripe.android.paymentsheet.addresselement

import java.lang.ref.WeakReference
import java.util.UUID

internal object CheckoutShippingAddressUpdaterRegistry {
    interface Updater {
        suspend fun update(address: AddressDetails): Result<Unit>
    }

    private val updaters = mutableMapOf<String, WeakReference<Updater>>()
    private val busyKeys = mutableSetOf<String>()

    @Synchronized
    fun register(updater: Updater): String {
        val key = UUID.randomUUID().toString()
        updaters[key] = WeakReference(updater)
        return key
    }

    @Synchronized
    fun get(key: String?): Updater? {
        if (key == null) return null
        val updater = updaters[key]?.get()
        if (updater == null) updaters.remove(key)
        return updater
    }

    @Synchronized
    fun remove(key: String?) {
        if (key != null) {
            updaters.remove(key)
            busyKeys.remove(key)
        }
    }

    @Synchronized
    fun setBusy(key: String?, busy: Boolean) {
        if (key == null) return
        if (busy) busyKeys.add(key) else busyKeys.remove(key)
    }

    @Synchronized
    fun isBusy(key: String?): Boolean = key != null && key in busyKeys
}
