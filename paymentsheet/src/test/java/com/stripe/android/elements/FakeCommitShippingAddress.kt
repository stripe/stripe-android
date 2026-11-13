@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.elements

import app.cash.turbine.Turbine
import com.stripe.android.checkout.CheckoutElementsController
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.coroutines.Deferred

internal class FakeCommitShippingAddress(
    private val result: Deferred<CheckoutElementsController.OperationResult>,
) : CommitShippingAddress {
    val calls = Turbine<Call>()

    override suspend fun invoke(
        name: String?,
        address: CheckoutElementsController.Address.State,
        checkoutSessionResponse: CheckoutSessionResponse,
    ): CheckoutElementsController.OperationResult {
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
