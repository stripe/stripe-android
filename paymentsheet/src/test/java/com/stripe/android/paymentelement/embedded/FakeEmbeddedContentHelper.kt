package com.stripe.android.paymentelement.embedded

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeEmbeddedContentHelper(
    override val embeddedContent: MutableStateFlow<EmbeddedContent?> = MutableStateFlow(null)
) : EmbeddedContentHelper {
    private val _dataLoadedTurbine = Turbine<PaymentMethodMetadata>()
    val dataLoadedTurbine: ReceiveTurbine<PaymentMethodMetadata> = _dataLoadedTurbine

    override fun dataLoaded(paymentMethodMetadata: PaymentMethodMetadata) {
        _dataLoadedTurbine.add(paymentMethodMetadata)
    }

    fun validate() {
        dataLoadedTurbine.ensureAllEventsConsumed()
    }
}
