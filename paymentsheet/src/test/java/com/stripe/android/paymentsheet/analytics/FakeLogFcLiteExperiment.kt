package com.stripe.android.paymentsheet.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.common.analytics.experiment.LogFcLiteExperiment
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ElementsSession

internal class FakeLogFcLiteExperiment : LogFcLiteExperiment {

    private val _calls = Turbine<Unit>()
    val calls: ReceiveTurbine<Unit> = _calls

    override fun invoke(elementsSession: ElementsSession, paymentMethodMetadata: PaymentMethodMetadata) {
        _calls.add(Unit)
    }
}
