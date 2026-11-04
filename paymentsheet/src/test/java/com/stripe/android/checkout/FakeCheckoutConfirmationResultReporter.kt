package com.stripe.android.checkout

import app.cash.turbine.Turbine
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

internal class FakeCheckoutConfirmationResultReporter : CheckoutConfirmationResultReporter {
    val calls = Turbine<Call>()

    override fun reportConfirmationResult(
        result: ConfirmationHandler.Result,
        response: CheckoutSessionResponse?,
    ) {
        calls.add(Call(result, response))
    }

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
    }

    data class Call(
        val result: ConfirmationHandler.Result,
        val response: CheckoutSessionResponse?,
    )
}
