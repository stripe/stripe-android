package com.stripe.android.checkout

import app.cash.turbine.Turbine
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.coroutines.Deferred

internal class FakeCommitSheetResult(
    private val result: Deferred<Result<Unit>>,
) : CommitSheetResult {
    val calls = Turbine<Call>()

    override suspend fun invoke(
        selection: PaymentSelection?,
        checkoutSessionResponse: CheckoutSessionResponse,
    ): Result<Unit> {
        calls.add(Call(selection, checkoutSessionResponse))
        return result.await()
    }

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
    }

    data class Call(
        val selection: PaymentSelection?,
        val checkoutSessionResponse: CheckoutSessionResponse,
    )
}
