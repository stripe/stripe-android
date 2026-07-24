package com.stripe.android.checkout

import app.cash.turbine.Turbine
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

@OptIn(CheckoutSessionPreview::class)
internal class FakeCheckoutStateLoader(
    private val reloadResult: Result<Unit> = Result.success(Unit),
) : CheckoutStateLoader {
    val loadInitialCalls = Turbine<LoadInitialCall>()
    val reloadCalls = Turbine<CheckoutControllerState>()

    override suspend fun loadInitial(
        configuration: CheckoutController.Configuration.State,
        checkoutSessionResponse: CheckoutSessionResponse,
    ) {
        loadInitialCalls.add(LoadInitialCall(configuration, checkoutSessionResponse))
    }

    override suspend fun reload(state: CheckoutControllerState) {
        reloadCalls.add(state)
        reloadResult.getOrThrow()
    }

    fun ensureAllEventsConsumed() {
        loadInitialCalls.ensureAllEventsConsumed()
        reloadCalls.ensureAllEventsConsumed()
    }

    data class LoadInitialCall(
        val configuration: CheckoutController.Configuration.State,
        val checkoutSessionResponse: CheckoutSessionResponse,
    )
}
