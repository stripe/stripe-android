package com.stripe.android.elements

import com.stripe.android.elements.ElementsController.ExpressCheckoutElement.ShippingAddress
import com.stripe.android.elements.ElementsController.ExpressCheckoutElement.ShippingAddressChangeEvent
import com.stripe.android.elements.ElementsController.ExpressCheckoutElement.ShippingChangeResult
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Applies merchant shipping decisions to the controller's order state for any wallet adapter. */
@Singleton
internal class ElementsShippingHandler @Inject constructor(
    private val stateHolder: ElementsControllerStateHolder,
    @PaymentElementCallbackIdentifier private val callbackIdentifier: String,
) {
    private val mutex = Mutex()

    suspend fun onAddressChange(address: ShippingAddress): ShippingChangeResult = update {
        val callback = PaymentElementCallbackReferences.getExact(callbackIdentifier)?.shippingAddressChangeCallback
        callback?.onShippingAddressChange(ShippingAddressChangeEvent(address))
            ?: ShippingChangeResult.Resolve(amount = null, lineItems = null)
    }

    private suspend fun update(
        block: suspend () -> ShippingChangeResult,
    ): ShippingChangeResult = withContext(Dispatchers.Main.immediate) {
        mutex.withLock {
            val loaded = checkNotNull(stateHolder.state) { "ElementsController is not configured." }
            val result = block()
            if (result is ShippingChangeResult.Resolve) {
                check(stateHolder.state?.configurationId == loaded.configurationId) {
                    "ElementsController was reconfigured during a shipping update."
                }
                val current = loaded.walletState
                val updated = current.copy(
                    amount = result.amount ?: current.amount,
                    lineItems = result.lineItems?.toList() ?: current.lineItems,
                )
                stateHolder.state = loaded.copy(walletState = updated)
                ShippingChangeResult.Resolve(updated.amount, updated.lineItems)
            } else {
                result
            }
        }
    }
}
