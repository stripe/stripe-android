package com.stripe.android.checkout

import androidx.annotation.VisibleForTesting
import com.stripe.android.paymentelement.CheckoutSessionPreview
import java.lang.ref.WeakReference

@OptIn(CheckoutSessionPreview::class)
internal object CheckoutInstances {
    private val instanceMap = mutableMapOf<String, WeakReference<Checkout>>()

    @Synchronized
    operator fun get(key: String): Checkout? {
        val checkout = instanceMap[key]?.get()
        if (checkout == null) instanceMap.remove(key)
        return checkout
    }

    @Synchronized
    fun getOrCreate(key: String, factory: () -> Checkout): Checkout {
        this[key]?.let { return it }
        val checkout = factory()
        instanceMap[key] = WeakReference(checkout)
        return checkout
    }

    @VisibleForTesting
    @Synchronized
    fun clear() {
        instanceMap.clear()
    }
}
