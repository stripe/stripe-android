package com.stripe.android.checkout

import app.cash.turbine.Turbine
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

internal class FakeCheckoutConfirmationFinalizer(
    var result: (CheckoutSessionResponse) -> CheckoutConfirmationFinalizer.Result = {
        CheckoutConfirmationFinalizer.Result.Completed(it)
    },
) : CheckoutConfirmationFinalizer {
    val calls = Turbine<CheckoutSessionResponse>()

    override suspend fun finalize(response: CheckoutSessionResponse): CheckoutConfirmationFinalizer.Result {
        calls.add(response)
        return result(response)
    }

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
    }
}
