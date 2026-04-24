package com.stripe.android.paymentsheet.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.common.analytics.experiment.CardArtExperimentHandler
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal class FakeCardArtExperimentHandler : CardArtExperimentHandler {

    private val _logExposureCalls = Turbine<LogExposureCall>()
    val logExposureCalls: ReceiveTurbine<LogExposureCall> = _logExposureCalls

    override fun logExposure(
        elementsSession: ElementsSession,
        paymentMethodMetadata: PaymentMethodMetadata,
        savedPaymentMethods: List<PaymentMethod>,
        integrationConfiguration: PaymentElementLoader.Configuration,
        defaultPaymentSelection: PaymentSelection?,
    ) {
        _logExposureCalls.add(
            LogExposureCall(
                elementsSession = elementsSession,
                paymentMethodMetadata = paymentMethodMetadata,
                savedPaymentMethods = savedPaymentMethods,
                integrationConfiguration = integrationConfiguration,
                defaultPaymentSelection = defaultPaymentSelection,
            )
        )
    }

    data class LogExposureCall(
        val elementsSession: ElementsSession,
        val paymentMethodMetadata: PaymentMethodMetadata,
        val savedPaymentMethods: List<PaymentMethod>,
        val integrationConfiguration: PaymentElementLoader.Configuration,
        val defaultPaymentSelection: PaymentSelection?,
    )
}
