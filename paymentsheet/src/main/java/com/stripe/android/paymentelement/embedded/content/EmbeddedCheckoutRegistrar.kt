package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.checkout.Checkout
import com.stripe.android.checkout.CheckoutInstances
import com.stripe.android.paymentelement.CheckoutSessionPreview
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds and manages the [Checkout] registration for the lifetime of one ViewModel-scoped
 * [EmbeddedPaymentElement].
 *
 * Registration happens in [EmbeddedPaymentElement.configure], and unregistration
 * happens when the owning ViewModel is cleared via [unregisterIfNeeded].
 */
@OptIn(CheckoutSessionPreview::class)
@Singleton
internal class EmbeddedCheckoutRegistrar @Inject constructor() {

    @Volatile
    private var registeredCheckout: Checkout? = null

    fun register(checkout: Checkout) {
        val previous = registeredCheckout
        if (previous != null && previous !== checkout) {
            CheckoutInstances.unregister(previous.internalState.key, previous)
        }
        CheckoutInstances.register(checkout.internalState.key, checkout, owner = this)
        registeredCheckout = checkout
    }

    fun unregisterIfNeeded() {
        val checkout = registeredCheckout ?: return
        CheckoutInstances.unregister(checkout.internalState.key, checkout)
        registeredCheckout = null
    }
}
