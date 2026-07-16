package com.stripe.android.checkout

import app.cash.turbine.Turbine

internal class FakeCheckoutConfirmationHelper : CheckoutConfirmationHelper {
    val confirmCalls = Turbine<Unit>()

    override fun confirm() {
        confirmCalls.add(Unit)
    }

    fun ensureAllEventsConsumed() {
        confirmCalls.ensureAllEventsConsumed()
    }
}
