package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutConfirmationStateUpdater internal constructor(
    private val stateHolder: CheckoutControllerStateHolder,
    private val fetchResponse: suspend (sessionId: String, adaptivePricingAllowed: Boolean) ->
        Result<CheckoutSessionResponse>,
    private val reloadState: suspend (CheckoutControllerState) -> Unit,
) {
    @Inject
    constructor(
        stateHolder: CheckoutControllerStateHolder,
        checkoutSessionRepository: CheckoutSessionRepository,
        checkoutStateLoader: CheckoutStateLoader,
    ) : this(
        stateHolder = stateHolder,
        fetchResponse = checkoutSessionRepository::init,
        reloadState = checkoutStateLoader::reload,
    )

    suspend fun update(checkoutSessionResponse: CheckoutSessionResponse?) {
        // The controller invokes this inside runSerialized, so the snapshot remains current across
        // the fetch and reload suspensions and cannot overwrite a concurrently committed mutation.
        val state = stateHolder.state ?: return
        val response = checkoutSessionResponse ?: runCatching {
            fetchResponse(
                state.checkoutSessionResponse.id,
                state.configuration.adaptivePricingAllowed,
            ).getOrThrow()
        }.getOrNull() ?: return

        // Reloading keeps every field derived from the response in sync. A refresh failure must not
        // turn a successfully completed payment into a failed result or prevent its callback.
        runCatching {
            reloadState(state.copy(checkoutSessionResponse = response))
        }
    }
}
