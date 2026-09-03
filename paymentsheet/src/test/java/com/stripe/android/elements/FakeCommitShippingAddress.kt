@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.elements

import app.cash.turbine.Turbine
import com.stripe.android.checkout.CheckoutController

internal class FakeCommitShippingAddress {
    val calls = Turbine<Call>()

    suspend operator fun invoke(
        name: String?,
        address: CheckoutController.Address.State,
    ): Result<Unit> {
        calls.add(Call(name, address))
        return Result.success(Unit)
    }

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
    }

    data class Call(
        val name: String?,
        val address: CheckoutController.Address.State,
    )
}
