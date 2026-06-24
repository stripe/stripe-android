package com.stripe.android.checkout

import androidx.annotation.VisibleForTesting
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
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

    // The factory runs under this lock. Checkout's init block calls add() which re-acquires the
    // same monitor (reentrant). This is intentional: it keeps the check-then-create atomic so two
    // concurrent callers cannot both create an instance for the same key.
    @Synchronized
    fun getOrCreate(key: String, factory: () -> Checkout): Checkout {
        this[key]?.let { return it }
        return factory()
    }

    @Synchronized
    fun add(key: String, checkout: Checkout) {
        instanceMap[key] = WeakReference(checkout)
    }

    @Synchronized
    fun ensureNoMutationInFlight(key: String) {
        this[key]?.ensureNoMutationInFlight()
    }

    @Synchronized
    fun markIntegrationLaunched(key: String) {
        this[key]?.markIntegrationLaunched()
    }

    @Synchronized
    fun markIntegrationDismissed(key: String) {
        this[key]?.markIntegrationDismissed()
    }

    fun markIntegrationDismissed(paymentMethodMetadata: PaymentMethodMetadata?) {
        val checkoutSession = paymentMethodMetadata
            ?.integrationMetadata as? IntegrationMetadata.CheckoutSession ?: return
        markIntegrationDismissed(checkoutSession.instancesKey)
    }

    @VisibleForTesting
    @Synchronized
    fun clear() {
        instanceMap.clear()
    }
}
