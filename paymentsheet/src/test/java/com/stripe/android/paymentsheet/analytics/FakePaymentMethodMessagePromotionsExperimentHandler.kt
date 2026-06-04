package com.stripe.android.paymentsheet.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.common.analytics.experiment.PaymentMethodMessagePromotionsExperimentHandler
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata

internal class FakePaymentMethodMessagePromotionsExperimentHandler : PaymentMethodMessagePromotionsExperimentHandler {

    private val _logExposureCalls = Turbine<Call>()
    val logExposureCalls: ReceiveTurbine<Call> = _logExposureCalls

    override fun logExposure(metadata: PaymentMethodMetadata) {
        _logExposureCalls.add(
            Call(metadata = metadata,)
        )
    }

    data class Call(
        val metadata: PaymentMethodMetadata
    )
}
