package com.stripe.android.checkout

import app.cash.turbine.Turbine
import com.stripe.android.paymentsheet.model.CheckoutSessionClientSecret
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

internal class FakeCheckoutSessionRefreshActions {
    val fetchCalls = Turbine<FetchCall>()
    val reloadCalls = Turbine<CheckoutElementsControllerLoadedState>()
    private val fetchResponses = Turbine<() -> Result<CheckoutSessionResponse>>()

    fun enqueueFetchResponse(response: () -> Result<CheckoutSessionResponse>) {
        fetchResponses.add(response)
    }

    suspend fun fetch(
        clientSecret: CheckoutSessionClientSecret,
        adaptivePricingAllowed: Boolean,
        analyticsParams: Map<String, Any?>,
    ): Result<CheckoutSessionResponse> {
        fetchCalls.add(FetchCall(clientSecret, adaptivePricingAllowed, analyticsParams))
        return fetchResponses.awaitItem().invoke()
    }

    suspend fun reload(state: CheckoutElementsControllerLoadedState) {
        reloadCalls.add(state)
    }

    fun ensureAllEventsConsumed() {
        fetchCalls.ensureAllEventsConsumed()
        reloadCalls.ensureAllEventsConsumed()
        fetchResponses.ensureAllEventsConsumed()
    }

    data class FetchCall(
        val clientSecret: CheckoutSessionClientSecret,
        val adaptivePricingAllowed: Boolean,
        val analyticsParams: Map<String, Any?>,
    )
}
