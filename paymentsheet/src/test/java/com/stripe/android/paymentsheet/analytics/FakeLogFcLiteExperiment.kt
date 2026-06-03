package com.stripe.android.paymentsheet.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.common.analytics.experiment.LogFcLiteExperiment
import com.stripe.android.model.ElementsSession

internal class FakeLogFcLiteExperiment : LogFcLiteExperiment {

    private val _calls = Turbine<ElementsSession>()
    val calls: ReceiveTurbine<ElementsSession> = _calls

    override fun invoke(elementsSession: ElementsSession) {
        _calls.add(elementsSession)
    }
}
