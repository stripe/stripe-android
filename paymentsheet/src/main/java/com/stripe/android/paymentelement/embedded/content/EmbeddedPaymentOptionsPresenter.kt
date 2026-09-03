package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CustomerStateHolder
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

internal interface EmbeddedPaymentOptionsPresenter {
    fun present()
    fun present(launchMode: EmbeddedLaunchMode)
}

internal class DefaultEmbeddedPaymentOptionsPresenter @Inject constructor(
    private val state: StateFlow<EmbeddedContentHelperStateHolder.State?>,
    private val sheetStateHolder: SheetStateHolder,
    private val customerStateHolder: CustomerStateHolder,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val errorReporter: ErrorReporter,
) : EmbeddedPaymentOptionsPresenter {
    override fun present() {
        val launchMode = if (state.value?.configuration?.preferForm == true) {
            EmbeddedLaunchMode.VerticalPaymentOptions
        } else {
            EmbeddedLaunchMode.PaymentOptions
        }
        present(launchMode)
    }

    override fun present(launchMode: EmbeddedLaunchMode) {
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
            launchMode = launchMode,
        )
    }
}
