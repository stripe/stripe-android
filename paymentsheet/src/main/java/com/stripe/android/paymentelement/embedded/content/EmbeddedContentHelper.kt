package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.InternalRowSelectionCallback
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.analytics.EventReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private val sheetStateHolder: SheetStateHolder,
    private val embeddedWalletsHelper: EmbeddedWalletsHelper,
    private val internalRowSelectionCallback: Provider<InternalRowSelectionCallback?>,
    private val customerStateHolder: CustomerStateHolder,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val errorReporter: ErrorReporter,
    private val preferFormInteractorFactory: EmbeddedPreferFormInteractorFactory,
    private val eventReporter: EventReporter,
) : EmbeddedContentHelper {

    private val _embeddedContent = MutableStateFlow<EmbeddedContent?>(null)
    override val embeddedContent: StateFlow<EmbeddedContent?> = _embeddedContent.asStateFlow()

    init {
        coroutineScope.launch {
            combine(state, selectionHolder.temporarySelection) { state, temporarySelection ->
                state to temporarySelection.takeIf { state?.configuration?.preferForm == true }
            }
                .distinctUntilChanged()
                .collect { (state, _) ->
                    val replacement = state?.let { currentState ->
                        val isImmediateAction = internalRowSelectionCallback.get() != null
                        val walletsState = embeddedWalletsHelper.walletsState(currentState.paymentMethodMetadata)
                        EmbeddedContent(
                            interactor = verticalLayoutInteractorFactory.create(
                                paymentMethodMetadata = currentState.paymentMethodMetadata,
                                configuration = currentState.configuration,
                                walletsState = walletsState,
                                isImmediateAction = isImmediateAction,
                                embeddedViewDisplaysMandateText = currentState.embeddedViewDisplaysMandateText,
                            ),
                            embeddedViewDisplaysMandateText = currentState.embeddedViewDisplaysMandateText,
                            appearance = currentState.configuration.appearance,
                            isImmediateAction = isImmediateAction,
                            preferFormInteractor = preferFormInteractorFactory.create(
                                paymentMethodMetadata = currentState.paymentMethodMetadata,
                                configuration = currentState.configuration,
                                walletsState = walletsState,
                                preferFormDisabled = currentState.preferFormDisabled,
                            ),
                            onMorePaymentMethods = {
                                launchPaymentOptions(currentState, EmbeddedLaunchMode.VerticalPaymentOptions)
                            },
                            eventReporter = eventReporter,
                            preferForm = currentState.configuration.preferForm,
                        )
                    }
                    _embeddedContent.value?.close()
                    _embeddedContent.value = replacement
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
        val launcher = sheetStateHolder.sheetLauncher
        if (launcher == null) {
            errorReporter.report(
                ErrorReporter.UnexpectedErrorEvent.EMBEDDED_PRESENT_PAYMENT_OPTIONS_NO_LAUNCHER
            )
            return
        }
        val launchMode = if (state.configuration.preferForm) {
            EmbeddedLaunchMode.VerticalPaymentOptions
        } else {
            EmbeddedLaunchMode.PaymentOptions
        }
        launchPaymentOptions(state, launchMode)
    }

    private fun launchPaymentOptions(
        state: EmbeddedContentHelperStateHolder.State,
        launchMode: EmbeddedLaunchMode,
    ) {
        val launcher = sheetStateHolder.sheetLauncher ?: return
        launcher.launchPaymentOptions(
            paymentMethodMetadata = state.paymentMethodMetadata,
            customerState = customerStateHolder.customer.value,
            selection = selectionHolder.selection.value,
            configuration = state.configuration,
            launchMode = launchMode,
        )
    }
}
