@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.elements

import app.cash.turbine.Turbine
import com.stripe.android.checkout.CheckoutController
import kotlinx.coroutines.Deferred

internal class FakeCommitShippingAddress(
    private val result: Deferred<Result<Unit>>,
) : CommitShippingAddress {
    val calls = Turbine<Call>()

    override suspend fun invoke(
        name: String?,
        address: CheckoutController.Address.State,
    ): Result<Unit> {
        calls.add(Call(name, address))
        return result.await()
    }

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
    }

    data class Call(
        val name: String?,
        val address: CheckoutController.Address.State,
    )
}
