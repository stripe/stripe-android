package com.stripe.android.paymentsheet.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.common.analytics.experiment.LogLinkHoldbackExperiment
import com.stripe.android.model.ElementsSession
import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal class FakeLogLinkHoldbackExperiment : LogLinkHoldbackExperiment {

    private val _calls = Turbine<Unit>()
    val calls: ReceiveTurbine<Unit> = _calls

    override fun invoke(
        elementsSession: ElementsSession,
        state: PaymentElementLoader.State
    ) {
        _calls.add(Unit)
    }
}
