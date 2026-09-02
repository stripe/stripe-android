package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject

/**
 * Reloads the controller's state after confirmation, either by fetching the current checkout
 * session or by committing a response returned by confirmation.
 */
internal interface CheckoutSessionRefresher {
    suspend fun refresh()

    suspend fun refresh(response: CheckoutSessionResponse)

    suspend fun refresh(response: CheckoutSessionResponse, selection: PaymentSelection?) {
        refresh(response)
    }
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
            state.configuration.currencySelectorElementConfiguration != null,
        ).getOrThrow()

        refresh(response)
    }

    override suspend fun refresh(response: CheckoutSessionResponse) {
        val state = stateHolder.state ?: return
        reloadState(state.copy(checkoutSessionResponse = response))
    }

    override suspend fun refresh(response: CheckoutSessionResponse, selection: PaymentSelection?) {
        val state = stateHolder.state ?: return
        reloadState(
            state.copy(
                checkoutSessionResponse = response,
                paymentSelection = selection,
            )
        )
    }
}
