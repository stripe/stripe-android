package com.stripe.android.checkout

import app.cash.turbine.Turbine
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

internal class FakeCheckoutSessionRefreshActions {
    val fetchCalls = Turbine<DefaultCheckoutSessionRefresher.FetchParams>()
    val reloadCalls = Turbine<CheckoutControllerState>()
    private val fetchResponses = Turbine<() -> Result<CheckoutSessionResponse>>()

    fun enqueueFetchResponse(response: () -> Result<CheckoutSessionResponse>) {
        fetchResponses.add(response)
    }

    suspend fun fetch(
        params: DefaultCheckoutSessionRefresher.FetchParams,
    ): Result<CheckoutSessionResponse> {
        fetchCalls.add(params)
        return fetchResponses.awaitItem().invoke()
    }

    suspend fun reload(state: CheckoutControllerState) {
        reloadCalls.add(state)
    }

    fun ensureAllEventsConsumed() {
        fetchCalls.ensureAllEventsConsumed()
        reloadCalls.ensureAllEventsConsumed()
        fetchResponses.ensureAllEventsConsumed()
    }
}
