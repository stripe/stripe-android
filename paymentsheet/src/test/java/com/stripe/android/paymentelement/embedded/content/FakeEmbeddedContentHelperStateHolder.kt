package com.stripe.android.paymentelement.embedded.content

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class FakeEmbeddedContentHelperStateHolder(
    _state: MutableStateFlow<EmbeddedContentHelperStateHolder.State?> = MutableStateFlow(null),
) : EmbeddedContentHelperStateHolder {
    override val state: StateFlow<EmbeddedContentHelperStateHolder.State?> = _state

    private val _dataLoadedTurbine = Turbine<EmbeddedContentHelperStateHolder.State>()
    val dataLoadedTurbine: ReceiveTurbine<EmbeddedContentHelperStateHolder.State> = _dataLoadedTurbine
    private val _clearEmbeddedContentTurbine = Turbine<Unit>()
    val clearEmbeddedContentTurbine: ReceiveTurbine<Unit> = _clearEmbeddedContentTurbine

    override fun dataLoaded(
        paymentMethodMetadata: PaymentMethodMetadata,
        appearance: Embedded,
        embeddedViewDisplaysMandateText: Boolean,
        configuration: EmbeddedPaymentElement.Configuration,
    ) {
        _dataLoadedTurbine.add(
            EmbeddedContentHelperStateHolder.State(
                paymentMethodMetadata = paymentMethodMetadata,
                appearance = appearance,
                embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
                configuration = configuration,
            )
        )
    }

    override fun clearEmbeddedContent() {
        _clearEmbeddedContentTurbine.add(Unit)
    }

    fun validate() {
        dataLoadedTurbine.ensureAllEventsConsumed()
        clearEmbeddedContentTurbine.ensureAllEventsConsumed()
    }
}
