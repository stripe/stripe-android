package com.stripe.android.checkout

import com.stripe.android.checkout.analytics.toAnalyticsMap
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
    suspend fun refresh()

    suspend fun refresh(response: CheckoutSessionResponse)
}

@OptIn(CheckoutSessionPreview::class)
internal class DefaultCheckoutSessionRefresher internal constructor(
    private val stateHolder: CheckoutElementsControllerStateHolder,
    private val fetchResponse: suspend (
        CheckoutSessionClientSecret,
        Boolean,
        Map<String, Any?>,
    ) -> Result<CheckoutSessionResponse>,
    private val reloadState: suspend (CheckoutElementsControllerLoadedState) -> Unit,
) : CheckoutSessionRefresher {

    @Inject
    internal constructor(
        stateHolder: CheckoutElementsControllerStateHolder,
        checkoutSessionRepository: CheckoutSessionRepository,
        checkoutStateLoader: CheckoutStateLoader,
    ) : this(
        stateHolder = stateHolder,
        fetchResponse = { clientSecret, adaptivePricingAllowed, analyticsParams ->
            checkoutSessionRepository.init(
                clientSecret = clientSecret,
                adaptivePricingAllowed = adaptivePricingAllowed,
                analyticsParams = analyticsParams,
            )
        },
        reloadState = checkoutStateLoader::reload,
    )

    override suspend fun refresh() {
        val state = stateHolder.state ?: return
        val response = fetchResponse(
            state.checkoutSessionResponse.clientSecret,
            state.configuration.currencySelectorElementConfiguration != null,
            state.configuration.toAnalyticsMap(),
        ).getOrThrow()

        refresh(response)
    }

    override suspend fun refresh(response: CheckoutSessionResponse) {
        val state = stateHolder.state ?: return
        reloadState(state.copy(checkoutSessionResponse = response))
    }
}
