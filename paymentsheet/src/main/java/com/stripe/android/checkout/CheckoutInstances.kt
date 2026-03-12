package com.stripe.android.checkout

import androidx.annotation.VisibleForTesting
import com.stripe.android.paymentelement.CheckoutSessionPreview
import java.lang.ref.WeakReference

@OptIn(CheckoutSessionPreview::class)
internal object CheckoutInstances {
    private val instanceMap = mutableMapOf<String, MutableList<WeakReference<Checkout>>>()

    operator fun get(key: String): List<Checkout> {
        val refs = instanceMap[key] ?: return emptyList()
        val live = refs.mapNotNull { it.get() }
        if (live.isEmpty()) {
            instanceMap.remove(key)
        } else if (live.size != refs.size) {
            // Prune stale references
            refs.clear()
            refs.addAll(live.map { WeakReference(it) })
        }
        return live
    }

    fun add(key: String, checkout: Checkout) {
        val refs = instanceMap.getOrPut(key) { mutableListOf() }
        refs.add(WeakReference(checkout))
    }

    fun remove(key: String) {
        instanceMap.remove(key)
    }

    @VisibleForTesting
    fun clear() {
        instanceMap.clear()
    }
}
