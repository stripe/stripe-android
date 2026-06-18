package com.stripe.android.checkout

import androidx.annotation.VisibleForTesting
import com.stripe.android.paymentelement.CheckoutSessionPreview

@OptIn(CheckoutSessionPreview::class)
internal object CheckoutInstances {
    private val instances = mutableMapOf<String, Checkout>()

    operator fun get(key: String): Checkout? = instances[key]

    fun register(key: String, checkout: Checkout) {
        val existing = instances[key]
        check(existing == null || existing === checkout) {
            "A different Checkout instance is already registered under key '$key'. " +
                "Close or reconfigure the existing integration before using a new Checkout with the same key."
        }
        instances[key] = checkout
    }

    fun unregister(key: String, checkout: Checkout) {
        if (instances[key] === checkout) {
            instances.remove(key)
        }
    }

    @VisibleForTesting
    fun clear() {
        instances.clear()
    }
}
