package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.InternalRowSelectionCallback
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CustomerStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

internal interface EmbeddedContentHelper {
    val embeddedContent: StateFlow<EmbeddedContent?>

    fun presentPaymentOptions()
}

internal class DefaultEmbeddedContentHelper @Inject constructor(
    @ViewModelScope private val coroutineScope: CoroutineScope,
    private val state: StateFlow<EmbeddedContentHelperStateHolder.State?>,
    private val verticalLayoutInteractorFactory: EmbeddedPaymentMethodVerticalLayoutInteractorFactory,
    private val sheetLauncherHolder: EmbeddedSheetLauncherHolder,
    private val embeddedWalletsHelper: EmbeddedWalletsHelper,
    private val internalRowSelectionCallback: Provider<InternalRowSelectionCallback?>,
    private val customerStateHolder: CustomerStateHolder,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val errorReporter: ErrorReporter,
) : EmbeddedContentHelper {

    private val _embeddedContent = MutableStateFlow<EmbeddedContent?>(null)
    override val embeddedContent: StateFlow<EmbeddedContent?> = _embeddedContent.asStateFlow()

    init {
        coroutineScope.launch {
            state.collect { state ->
                _embeddedContent.value = if (state == null) {
                    null
                } else {
                    val isImmediateAction = internalRowSelectionCallback.get() != null
                    EmbeddedContent(
                        interactor = verticalLayoutInteractorFactory.create(
                            paymentMethodMetadata = state.paymentMethodMetadata,
                            configuration = state.configuration,
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
    }

    override fun presentPaymentOptions() {
        val state = state.value
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
            configuration = state.configuration,
        )
    }
}
