package com.stripe.android.checkout

import app.cash.turbine.Turbine
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

internal class FakeCheckoutConfirmationFinalizer(
    var result: (CheckoutSessionResponse) -> Result<CheckoutSessionResponse> = { Result.success(it) },
) : CheckoutConfirmationFinalizer {
    val calls = Turbine<CheckoutSessionResponse>()

    override suspend fun finalize(response: CheckoutSessionResponse): Result<CheckoutSessionResponse> {
        calls.add(response)
        return result(response)
    }

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
    }
}
