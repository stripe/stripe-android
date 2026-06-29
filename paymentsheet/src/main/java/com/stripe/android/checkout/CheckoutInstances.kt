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

    @Synchronized
    fun getOrCreate(key: String, factory: () -> Checkout): Checkout {
        this[key]?.let { return it }
        val checkout = factory()
        instanceMap[key] = WeakReference(checkout)
        return checkout
    }

    @Synchronized
    fun ensureNoMutationInFlight(key: String) {
        this[key]?.ensureNoMutationInFlight()
    }

    suspend fun <T> withConfirmation(key: String, block: suspend () -> T): T {
        val checkout = this[key]
        return if (checkout != null) {
            checkout.withConfirmation(block)
        } else {
            block()
        }
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
