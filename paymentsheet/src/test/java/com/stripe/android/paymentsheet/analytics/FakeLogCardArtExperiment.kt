package com.stripe.android.paymentsheet.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.common.analytics.experiment.LogCardArtExperiment
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal class FakeLogCardArtExperiment(
    private val isEnabled: Boolean = false,
) : LogCardArtExperiment {

    private val _calls = Turbine<Call>()
    val calls: ReceiveTurbine<Call> = _calls

    override fun invoke(
        elementsSession: ElementsSession,
        paymentMethodMetadata: PaymentMethodMetadata,
        savedPaymentMethods: List<PaymentMethod>,
        integrationConfiguration: PaymentElementLoader.Configuration,
        defaultPaymentSelection: PaymentSelection?,
    ): Boolean {
        _calls.add(
            Call(
                elementsSession = elementsSession,
                paymentMethodMetadata = paymentMethodMetadata,
                savedPaymentMethods = savedPaymentMethods,
                integrationConfiguration = integrationConfiguration,
                defaultPaymentSelection = defaultPaymentSelection,
            )
        )
        return isEnabled
    }

    data class Call(
        val elementsSession: ElementsSession,
        val paymentMethodMetadata: PaymentMethodMetadata,
        val savedPaymentMethods: List<PaymentMethod>,
        val integrationConfiguration: PaymentElementLoader.Configuration,
        val defaultPaymentSelection: PaymentSelection?,
    )
}
