package com.stripe.android.paymentelement.embedded.content

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal class FakeEmbeddedContentHelper(
    override val embeddedContent: MutableStateFlow<EmbeddedContent?> = MutableStateFlow(null)
) : EmbeddedContentHelper {
    private val _dataLoadedTurbine = Turbine<DefaultEmbeddedContentHelper.State>()
    val dataLoadedTurbine: ReceiveTurbine<DefaultEmbeddedContentHelper.State> = _dataLoadedTurbine
    private val _clearEmbeddedContentTurbine = Turbine<Unit>()
    val clearEmbeddedContentTurbine: ReceiveTurbine<Unit> = _clearEmbeddedContentTurbine

    var testSheetLauncher: EmbeddedSheetLauncher? = null

    override fun dataLoaded(
        paymentMethodMetadata: PaymentMethodMetadata,
        rowStyle: Embedded.RowStyle,
        embeddedViewDisplaysMandateText: Boolean,
    ) {
        _dataLoadedTurbine.add(
            DefaultEmbeddedContentHelper.State(
                paymentMethodMetadata = paymentMethodMetadata,
                rowStyle = rowStyle,
                embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
            )
        )
    }

    override fun clearEmbeddedContent() {
        _clearEmbeddedContentTurbine.add(Unit)
    }

    override fun setSheetLauncher(sheetLauncher: EmbeddedSheetLauncher) {
        this.testSheetLauncher = sheetLauncher
    }

    override fun clearSheetLauncher() {
        testSheetLauncher = null
    }

    fun validate() {
        dataLoadedTurbine.ensureAllEventsConsumed()
        clearEmbeddedContentTurbine.ensureAllEventsConsumed()
    }
}
