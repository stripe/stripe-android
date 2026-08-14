package com.stripe.android.checkout

import app.cash.turbine.Turbine

internal class FakeCheckoutSessionRefresher : CheckoutSessionRefresher {
    val calls = Turbine<Call>()
    private val refreshActions = Turbine<suspend () -> Unit>()

    fun enqueueRefreshAction(action: suspend () -> Unit) {
        refreshActions.add(action)
    }

    override suspend fun refresh() {
        record(Call.Fetch)
    }

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
        refreshActions.ensureAllEventsConsumed()
    }

    private suspend fun record(call: Call) {
        calls.add(call)
        refreshActions.awaitItem().invoke()
    }

    sealed interface Call {
        data object Fetch : Call
    }
}
