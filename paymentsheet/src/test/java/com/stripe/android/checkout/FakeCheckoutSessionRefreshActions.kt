package com.stripe.android.checkout

import app.cash.turbine.Turbine
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

internal class FakeCheckoutSessionRefreshActions {
    val fetchCalls = Turbine<FetchCall>()
    val reloadCalls = Turbine<CheckoutControllerState>()
    private val fetchResponses = Turbine<() -> Result<CheckoutSessionResponse>>()

    fun enqueueFetchResponse(response: () -> Result<CheckoutSessionResponse>) {
        fetchResponses.add(response)
    }

    suspend fun fetch(
        clientSecret: String,
        adaptivePricingAllowed: Boolean,
    ): Result<CheckoutSessionResponse> {
        fetchCalls.add(FetchCall(clientSecret, adaptivePricingAllowed))
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

    data class FetchCall(
        val clientSecret: String,
        val adaptivePricingAllowed: Boolean,
    )
}
