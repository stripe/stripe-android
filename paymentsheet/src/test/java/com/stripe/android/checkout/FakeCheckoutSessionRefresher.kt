package com.stripe.android.checkout

import app.cash.turbine.Turbine
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

internal class FakeCheckoutSessionRefresher : CheckoutSessionRefresher {
    val calls = Turbine<Call>()
    private val refreshActions = Turbine<suspend () -> Unit>()

    fun enqueueRefreshAction(action: suspend () -> Unit) {
        refreshActions.add(action)
    }

    override suspend fun refresh() {
        record(Call.Fetch)
    }

    override suspend fun refresh(response: CheckoutSessionResponse) {
        record(Call.Commit(response))
    }

    override suspend fun refresh(response: CheckoutSessionResponse, selection: PaymentSelection?) {
        record(Call.CommitWithSelection(response, selection))
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
        data class Commit(val response: CheckoutSessionResponse) : Call
        data class CommitWithSelection(
            val response: CheckoutSessionResponse,
            val selection: PaymentSelection?,
        ) : Call
    }
}
