package com.stripe.android.common.analytics.experiment

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal class FakePreselectLinkExperiment(
    private val selection: PaymentSelection.Link? = null,
) : PreselectLinkExperiment {
    private val _getSelectionCalls = Turbine<Call>()
    val getSelectionCalls: ReceiveTurbine<Call> = _getSelectionCalls

    override fun getSelection(
        metadata: PaymentMethodMetadata,
        customer: CustomerState?,
        integrationConfiguration: PaymentElementLoader.Configuration,
    ): PaymentSelection.Link? {
        _getSelectionCalls.add(Call(metadata, customer, integrationConfiguration))
        return selection
    }

    fun validate() {
        _getSelectionCalls.ensureAllEventsConsumed()
    }

    data class Call(
        val metadata: PaymentMethodMetadata,
        val customer: CustomerState?,
        val integrationConfiguration: PaymentElementLoader.Configuration,
    )
}
