package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.ui.WalletButtonsContent
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

internal interface EmbeddedContentHelper {
    val embeddedContent: StateFlow<EmbeddedContent?>
    val walletButtonsContent: StateFlow<WalletButtonsContent?>

    fun setSheetLauncher(sheetLauncher: EmbeddedSheetLauncher)

    fun clearSheetLauncher()

    fun presentPaymentOptions()
}

/**
 * A thin facade over [EmbeddedContentHelperDataSource]: it exposes the derived content flows and owns
 * the imperatively-set [EmbeddedSheetLauncher] (stored in [EmbeddedSheetLauncherHolder]) that the
 * data source's interactors and [presentPaymentOptions] launch through.
 */
@Singleton
internal class DefaultEmbeddedContentHelper @Inject constructor(
    private val dataSource: EmbeddedContentHelperDataSource,
    private val errorReporter: ErrorReporter,
    private val customerStateHolder: CustomerStateHolder,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val sheetLauncherHolder: EmbeddedSheetLauncherHolder,
) : EmbeddedContentHelper {

    override val embeddedContent: StateFlow<EmbeddedContent?> = dataSource.embeddedContent

    override val walletButtonsContent: StateFlow<WalletButtonsContent?> = dataSource.walletButtonsContent

    override fun setSheetLauncher(sheetLauncher: EmbeddedSheetLauncher) {
        sheetLauncherHolder.sheetLauncher = sheetLauncher
    }

    override fun clearSheetLauncher() {
        sheetLauncherHolder.sheetLauncher = null
    }

    override fun presentPaymentOptions() {
        val confirmationState = dataSource.embeddedConfirmationState.value
        if (confirmationState == null) {
            errorReporter.report(
                ErrorReporter.UnexpectedErrorEvent.EMBEDDED_PRESENT_PAYMENT_OPTIONS_NOT_CONFIGURED
            )
            return
        }
        val launcher = sheetLauncherHolder.sheetLauncher
        if (launcher == null) {
            errorReporter.report(
                ErrorReporter.UnexpectedErrorEvent.EMBEDDED_PRESENT_PAYMENT_OPTIONS_NO_LAUNCHER
            )
            return
        }
        launcher.launchPaymentOptions(
            paymentMethodMetadata = confirmationState.paymentMethodMetadata,
            customerState = customerStateHolder.customer.value,
            selection = selectionHolder.selection.value,
            embeddedConfirmationState = confirmationState,
        )
    }
}
