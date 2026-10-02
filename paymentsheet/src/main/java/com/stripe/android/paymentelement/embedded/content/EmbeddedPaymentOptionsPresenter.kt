package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CustomerStateHolder
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

internal interface EmbeddedPaymentOptionsPresenter {
    fun present()
}

internal class DefaultEmbeddedPaymentOptionsPresenter @Inject constructor(
    private val state: StateFlow<EmbeddedContentHelperStateHolder.State?>,
    private val sheetStateHolder: SheetStateHolder,
    private val customerStateHolder: CustomerStateHolder,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val errorReporter: ErrorReporter,
) : EmbeddedPaymentOptionsPresenter {
    override fun present() {
        val state = state.value
        if (state == null) {
            errorReporter.report(
                ErrorReporter.UnexpectedErrorEvent.EMBEDDED_PRESENT_PAYMENT_OPTIONS_NOT_CONFIGURED
            )
            return
        }
        val launcher = sheetStateHolder.sheetLauncher
        if (launcher == null) {
            errorReporter.report(
                ErrorReporter.UnexpectedErrorEvent.EMBEDDED_PRESENT_PAYMENT_OPTIONS_NO_LAUNCHER
            )
            return
        }
        launcher.launchPaymentOptions(
            paymentMethodMetadata = state.paymentMethodMetadata,
            customerState = customerStateHolder.customer.value,
            selection = selectionHolder.selection.value,
            configuration = state.configuration,
        )
    }
}
