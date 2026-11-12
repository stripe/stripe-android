package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.model.CheckoutSessionClientSecret
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject

/**
 * Reloads the controller's state after confirmation, either by fetching the current checkout
 * session or by committing a response returned by confirmation.
 */
internal interface CheckoutSessionRefresher {
    suspend fun refresh(): CheckoutSessionResponse?

    suspend fun refresh(response: CheckoutSessionResponse)
}

@OptIn(CheckoutSessionPreview::class)
internal class DefaultCheckoutSessionRefresher internal constructor(
    private val stateHolder: CheckoutElementsControllerStateHolder,
    private val fetchResponse: suspend (CheckoutSessionClientSecret) -> Result<CheckoutSessionResponse>,
    private val reloadState: suspend (CheckoutElementsControllerState) -> Unit,
) : CheckoutSessionRefresher {

    @Inject
    internal constructor(
        stateHolder: CheckoutElementsControllerStateHolder,
        checkoutSessionRepository: CheckoutSessionRepository,
        checkoutStateLoader: CheckoutStateLoader,
    ) : this(
        stateHolder = stateHolder,
        fetchResponse = checkoutSessionRepository::fetch,
        reloadState = checkoutStateLoader::reload,
    )

    override suspend fun refresh(): CheckoutSessionResponse? {
        val state = stateHolder.state ?: return null
        val response = fetchResponse(state.checkoutSessionResponse.clientSecret).getOrThrow()

        refresh(response)
        return response
    }

    override suspend fun refresh(response: CheckoutSessionResponse) {
        val state = stateHolder.state ?: return
        reloadState(state.copy(checkoutSessionResponse = response))
    }
}
