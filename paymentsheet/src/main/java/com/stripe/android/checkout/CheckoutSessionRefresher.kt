package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import javax.inject.Inject

/**
 * Re-fetches the current checkout session and reloads the controller's state from the fresh
 * response, so state that was loaded before a confirmation doesn't outlive it.
 */
internal fun interface CheckoutSessionRefresher {
    suspend fun refresh()
}

@OptIn(CheckoutSessionPreview::class)
internal class DefaultCheckoutSessionRefresher @Inject constructor(
    private val stateHolder: CheckoutControllerStateHolder,
    private val checkoutSessionRepository: CheckoutSessionRepository,
    private val checkoutStateLoader: CheckoutStateLoader,
) : CheckoutSessionRefresher {
    override suspend fun refresh() {
        val state = stateHolder.state ?: return
        val response = checkoutSessionRepository.init(
            sessionId = state.checkoutSessionResponse.id,
            adaptivePricingAllowed = state.configuration.adaptivePricingAllowed,
        ).getOrThrow()
        checkoutStateLoader.reload(state.copy(checkoutSessionResponse = response))
    }
}
