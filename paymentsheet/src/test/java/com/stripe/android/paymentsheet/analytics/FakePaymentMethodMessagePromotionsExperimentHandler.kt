package com.stripe.android.paymentsheet.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.common.analytics.experiment.PaymentMethodMessagePromotionsExperimentHandler
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodMessagePromotion

internal class FakePaymentMethodMessagePromotionsExperimentHandler : PaymentMethodMessagePromotionsExperimentHandler {

    private val _logExposureCalls = Turbine<Call>()
    val logExposureCalls: ReceiveTurbine<Call> = _logExposureCalls

    override fun logExposure(
        code: PaymentMethodCode,
        metadata: PaymentMethodMetadata,
        promotion: PaymentMethodMessagePromotion?
    ) {
        _logExposureCalls.add(
            Call(
                code = code,
                metadata = metadata,
                promotion = promotion
            )
        )
    }

    data class Call(
        val code: PaymentMethodCode,
        val metadata: PaymentMethodMetadata,
        val promotion: PaymentMethodMessagePromotion?,
    )
}
