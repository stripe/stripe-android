@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.elements

import app.cash.turbine.Turbine
import com.stripe.android.checkout.CheckoutElementsController
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.coroutines.Deferred

internal class FakeCommitShippingAddress(
    private val result: Deferred<Result<Unit>>,
) : CommitShippingAddress {
    val calls = Turbine<Call>()

    override suspend fun invoke(
        name: String?,
        address: CheckoutElementsController.Address.State,
        checkoutSessionResponse: CheckoutSessionResponse,
    ): Result<Unit> {
        calls.add(Call(name, address, checkoutSessionResponse))
        return result.await()
    }

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
    }

    data class Call(
        val name: String?,
        val address: CheckoutElementsController.Address.State,
        val checkoutSessionResponse: CheckoutSessionResponse,
    )
}
