@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.elements

import app.cash.turbine.Turbine
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.coroutines.Deferred

internal class FakeCommitShippingAddress(
    private val result: Deferred<Result<Unit>>,
) : CommitShippingAddress {
    val calls = Turbine<Call>()

    override suspend fun invoke(
        updatedCheckoutSessionResponse: CheckoutSessionResponse,
        name: String?,
        address: CheckoutController.Address.State,
    ): Result<Unit> {
        calls.add(Call(updatedCheckoutSessionResponse, name, address))
        return result.await()
    }

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
    }

    data class Call(
        val checkoutSessionResponse: CheckoutSessionResponse,
        val name: String?,
        val address: CheckoutController.Address.State,
    )
}
