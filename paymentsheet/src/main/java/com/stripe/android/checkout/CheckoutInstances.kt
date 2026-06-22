package com.stripe.android.checkout

import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import com.stripe.android.paymentelement.CheckoutSessionPreview

@OptIn(CheckoutSessionPreview::class)
internal object CheckoutInstances {

    private data class Entry(val checkout: Checkout, val owner: String)

    private val instances = mutableMapOf<String, Entry>()

    @MainThread
    operator fun get(key: String): Checkout? = instances[key]?.checkout

    @MainThread
    fun register(key: String, checkout: Checkout, owner: String) {
        val existing = instances[key]
        if (existing != null) {
            check(existing.checkout === checkout && existing.owner == owner) {
                "Checkout already registered by '${existing.owner}'. " +
                    "Only one integration can use a Checkout at a time."
            }
        }
        instances[key] = Entry(checkout, owner)
    }

    @MainThread
    fun unregister(key: String, checkout: Checkout) {
        if (instances[key]?.checkout === checkout) {
            instances.remove(key)
        }
    }

    @VisibleForTesting
    fun clear() {
        instances.clear()
    }
}
