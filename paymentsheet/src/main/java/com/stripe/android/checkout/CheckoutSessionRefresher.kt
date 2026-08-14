package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject

/**
 * Re-fetches the current checkout session and reloads the controller's state from the fresh
 * response, so state that was loaded before a confirmation doesn't outlive it.
 */
internal fun interface CheckoutSessionRefresher {
    suspend fun refresh()
}

@OptIn(CheckoutSessionPreview::class)
internal class DefaultCheckoutSessionRefresher internal constructor(
    private val stateHolder: CheckoutControllerStateHolder,
    private val fetchResponse: suspend (String, Boolean) -> Result<CheckoutSessionResponse>,
    private val reloadState: suspend (CheckoutControllerState) -> Unit,
) : CheckoutSessionRefresher {

    @Inject
    internal constructor(
        stateHolder: CheckoutControllerStateHolder,
        checkoutSessionRepository: CheckoutSessionRepository,
        checkoutStateLoader: CheckoutStateLoader,
    ) : this(
        stateHolder = stateHolder,
        fetchResponse = checkoutSessionRepository::init,
        reloadState = checkoutStateLoader::reload,
    )

    override suspend fun refresh() {
        val state = stateHolder.state ?: return
        val response = fetchResponse(
            state.checkoutSessionResponse.id,
            state.configuration.adaptivePricingAllowed,
        ).getOrThrow()
        val latestState = stateHolder.state ?: return
        reloadState(latestState.copy(checkoutSessionResponse = response))
    }
}
