package com.stripe.android.paymentsheet.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.common.analytics.experiment.LogCardArtExperiment
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ElementsSession

internal class FakeLogCardArtExperiment : LogCardArtExperiment {

    private val _calls = Turbine<Call>()
    val calls: ReceiveTurbine<Call> = _calls

    override fun invoke(
        elementsSession: ElementsSession,
        paymentMethodMetadata: PaymentMethodMetadata,
    ) {
        _calls.add(
            Call(
                elementsSession = elementsSession,
                paymentMethodMetadata = paymentMethodMetadata,
            )
        )
    }

    data class Call(
        val elementsSession: ElementsSession,
        val paymentMethodMetadata: PaymentMethodMetadata,
    )
}
