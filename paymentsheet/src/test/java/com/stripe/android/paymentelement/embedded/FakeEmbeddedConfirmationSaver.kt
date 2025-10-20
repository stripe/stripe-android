package com.stripe.android.paymentelement.embedded

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.model.StripeIntent

internal class FakeEmbeddedConfirmationSaver : EmbeddedConfirmationSaver {
    private val _saveTurbine = Turbine<StripeIntent>()
    val saveTurbine: ReceiveTurbine<StripeIntent> = _saveTurbine

    override fun save(stripeIntent: StripeIntent) {
        _saveTurbine.add(stripeIntent)
    }

    fun validate() {
        _saveTurbine.ensureAllEventsConsumed()
    }
}
