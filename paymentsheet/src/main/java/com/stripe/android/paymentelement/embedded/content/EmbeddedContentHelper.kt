package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.InternalRowSelectionCallback
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.ui.WalletButtonsContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

internal interface EmbeddedContentHelper {
    val embeddedContent: StateFlow<EmbeddedContent?>
    val walletButtonsContent: StateFlow<WalletButtonsContent?>

    fun presentPaymentOptions()
}

@Singleton
internal class DefaultEmbeddedContentHelper @Inject constructor(
    @ViewModelScope private val coroutineScope: CoroutineScope,
    private val stateHolder: EmbeddedContentHelperStateHolder,
    private val verticalLayoutInteractorFactory: EmbeddedPaymentMethodVerticalLayoutInteractorFactory,
    private val walletButtonsInteractorFactory: EmbeddedWalletButtonsInteractorFactory,
    private val sheetLauncherHolder: EmbeddedSheetLauncherHolder,
    private val embeddedWalletsHelper: EmbeddedWalletsHelper,
    private val internalRowSelectionCallback: Provider<InternalRowSelectionCallback?>,
    private val customerStateHolder: CustomerStateHolder,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val confirmationStateHolder: EmbeddedConfirmationStateHolder,
    private val errorReporter: ErrorReporter,
) : EmbeddedContentHelper {

    private val _embeddedContent = MutableStateFlow<EmbeddedContent?>(null)
    override val embeddedContent: StateFlow<EmbeddedContent?> = _embeddedContent.asStateFlow()

    private val _walletButtonsContent = MutableStateFlow<WalletButtonsContent?>(null)
    override val walletButtonsContent: StateFlow<WalletButtonsContent?> = _walletButtonsContent.asStateFlow()

    init {
        coroutineScope.launch {
            stateHolder.state.collect { state ->
                _embeddedContent.value = if (state == null) {
                    null
                } else {
                    val isImmediateAction = internalRowSelectionCallback.get() != null
                    EmbeddedContent(
                        interactor = verticalLayoutInteractorFactory.create(
                            paymentMethodMetadata = state.paymentMethodMetadata,
                            walletsState = embeddedWalletsHelper.walletsState(state.paymentMethodMetadata),
                            isImmediateAction = isImmediateAction,
                            embeddedViewDisplaysMandateText = state.embeddedViewDisplaysMandateText,
                        ),
                        embeddedViewDisplaysMandateText = state.embeddedViewDisplaysMandateText,
                        appearance = state.appearance,
                        isImmediateAction = isImmediateAction,
                    )
                }
            }
        }

        coroutineScope.launch {
            stateHolder.state.collect { state ->
                _walletButtonsContent.value = if (state == null) {
                    null
                } else {
                    WalletButtonsContent(
                        interactor = walletButtonsInteractorFactory.create(),
                    )
                }
            }
        }
    }

    override fun presentPaymentOptions() {
        val state = stateHolder.state.value
        if (state == null) {
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
            paymentMethodMetadata = state.paymentMethodMetadata,
            customerState = customerStateHolder.customer.value,
            selection = selectionHolder.selection.value,
            embeddedConfirmationState = confirmationStateHolder.state,
        )
    }
}
