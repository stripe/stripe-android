package com.stripe.android.paymentelement.embedded.sheet

import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.common.taptoadd.TapToAddHelper
import com.stripe.android.common.taptoadd.TapToAddNextStep
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.form.OnClickOverrideDelegate
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.amount
import com.stripe.android.paymentsheet.model.currency
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import com.stripe.android.paymentsheet.utils.buyButtonLabel
import com.stripe.android.paymentsheet.utils.continueButtonLabel
import com.stripe.android.paymentsheet.utils.reportPaymentResult
import com.stripe.android.ui.core.Amount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

internal interface SheetActivityStateHolder {
    val state: StateFlow<State>
    val result: SharedFlow<EmbeddedActivityResult>
    val validationRequested: SharedFlow<Unit>
    fun updateMandate(mandateText: ResolvableString?)
    fun updatePrimaryButton(callback: (PrimaryButton.UIState?) -> PrimaryButton.UIState?)
    fun updateError(error: ResolvableString?)
    fun updateProcessing(isProcessing: Boolean)
    fun onPrimaryButtonDisabledClick()
    fun selectSavedPaymentMethod(selection: PaymentSelection.Saved)

    fun setResult(result: EmbeddedActivityResult)

    data class State(
        val primaryButtonLabel: ResolvableString,
        val isEnabled: Boolean,
        val processingState: PrimaryButtonProcessingState,
        val isProcessing: Boolean,
        val shouldDisplayLockIcon: Boolean,
        val error: ResolvableString? = null,
        val mandateText: ResolvableString? = null,
        val pendingPaymentMethodId: String? = null,
    )
}

@Singleton
internal class DefaultSheetActivityStateHolder @Inject constructor(
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val configuration: EmbeddedPaymentElement.Configuration,
    private val onClickDelegate: OnClickOverrideDelegate,
    private val eventReporter: EventReporter,
    @param:ViewModelScope private val coroutineScope: CoroutineScope,
    private val confirmationHandler: ConfirmationHandler,
    private val tapToAddHelper: TapToAddHelper,
    private val customerStateHolder: CustomerStateHolder,
    private val launchMode: EmbeddedLaunchMode,
    private val embeddedNavigatorProvider: Provider<EmbeddedNavigator>,
    private val savedPaymentMethodConfirmScreenFactoryProvider: Provider<SavedPaymentMethodConfirmScreenFactory>,
    private val sheetTaxRegionUpdaterProvider: Provider<SheetTaxRegionUpdater> = Provider { error("Not expected") },
) : SheetActivityStateHolder {
    private val _state = MutableStateFlow(
        SheetActivityStateHolder.State(
            primaryButtonLabel = primaryButtonLabel(paymentMethodMetadata.stripeIntent, configuration),
            isEnabled = false,
            processingState = PrimaryButtonProcessingState.Idle(null),
            isProcessing = false,
            shouldDisplayLockIcon = launchMode !is EmbeddedLaunchMode.PaymentOptions &&
                configuration.formSheetAction == EmbeddedPaymentElement.FormSheetAction.Confirm,
        )
    )
    override val state: StateFlow<SheetActivityStateHolder.State> = _state

    private val _result = MutableSharedFlow<EmbeddedActivityResult>()
    override val result: SharedFlow<EmbeddedActivityResult> = _result

    private val _validationRequested = MutableSharedFlow<Unit>()
    override val validationRequested: SharedFlow<Unit> = _validationRequested

    private var usBankAccountFormPrimaryButtonUiState: PrimaryButton.UIState? = null

    init {
        coroutineScope.launch {
            tapToAddHelper.nextStep.collect { result ->
                val activityResult = when (result) {
                    is TapToAddNextStep.ConfirmSavedPaymentMethod -> {
                        val screen = savedPaymentMethodConfirmScreenFactoryProvider.get().create(
                            selection = result.paymentSelection,
                        )
                        embeddedNavigatorProvider.get().performAction(
                            EmbeddedNavigator.Action.ReplaceCurrentScreen(screen)
                        )
                        null
                    }
                    is TapToAddNextStep.ShowSavedPaymentMethods -> EmbeddedActivityResult.Complete(
                        selection = result.paymentSelection,
                        previousNewSelections = selectionHolder.previousNewSelections,
                        hasBeenConfirmed = false,
                        customerState = customerStateHolder.customer.value,
                        checkoutSessionResponse = null,
                        shouldInvokeSelectionCallback = false,
                        launchMode = launchMode,
                    )
                    TapToAddNextStep.Complete -> EmbeddedActivityResult.Complete(
                        selection = null,
                        previousNewSelections = selectionHolder.previousNewSelections,
                        hasBeenConfirmed = true,
                        customerState = customerStateHolder.customer.value,
                        checkoutSessionResponse = null,
                        shouldInvokeSelectionCallback = false,
                        launchMode = launchMode,
                    )
                    is TapToAddNextStep.Continue -> {
                        customerStateHolder.addPaymentMethod(result.paymentSelection.paymentMethod)
                        EmbeddedActivityResult.Complete(
                            selection = result.paymentSelection,
                            previousNewSelections = selectionHolder.previousNewSelections,
                            hasBeenConfirmed = false,
                            customerState = customerStateHolder.customer.value,
                            checkoutSessionResponse = null,
                            shouldInvokeSelectionCallback = false,
                            launchMode = launchMode,
                        )
                    }
                }
                activityResult?.let(::setResult)
            }
        }

        coroutineScope.launch {
            confirmationHandler.state.collectLatest { confirmationState ->
                _state.update {
                    it.updateWithConfirmationState(confirmationState)
                }
            }
        }

        coroutineScope.launch {
            selectionHolder.selection.collectLatest { selection ->
                _state.update { currentState ->
                    currentState.copy(
                        isEnabled = usBankAccountFormPrimaryButtonUiState?.enabled
                            ?: (selection != null && !currentState.isProcessing)
                    )
                }
            }
        }
    }

    override fun setResult(result: EmbeddedActivityResult) {
        coroutineScope.launch {
            _result.emit(result)
        }
    }

    override fun updateMandate(mandateText: ResolvableString?) {
        _state.update {
            it.copy(
                mandateText = mandateText
            )
        }
    }

    override fun updateError(error: ResolvableString?) {
        _state.update {
            it.copy(
                error = error
            )
        }
    }

    override fun updateProcessing(isProcessing: Boolean) {
        _state.update {
            if (isProcessing) {
                it.copy(
                    isProcessing = true,
                    processingState = PrimaryButtonProcessingState.Processing,
                    isEnabled = false,
                    error = null,
                )
            } else {
                it.copy(
                    isProcessing = false,
                    processingState = PrimaryButtonProcessingState.Idle(null),
                    isEnabled = selectionHolder.selection.value != null,
                )
            }
        }
    }

    override fun onPrimaryButtonDisabledClick() {
        val primaryButtonUiState = usBankAccountFormPrimaryButtonUiState
        if (primaryButtonUiState != null) {
            if (primaryButtonUiState.canClickWhileDisabled) {
                primaryButtonUiState.onDisabledClick()
            }
        } else if (!_state.value.isProcessing) {
            coroutineScope.launch {
                _validationRequested.emit(Unit)
            }
        }
    }

    override fun selectSavedPaymentMethod(selection: PaymentSelection.Saved) {
        if (_state.value.isProcessing) return

        _state.update { it.copy(pendingPaymentMethodId = selection.paymentMethod.id, error = null) }

        val update = sheetTaxRegionUpdaterProvider.get().prepareUpdate(paymentMethodMetadata, selection)
        if (update == null) {
            selectionHolder.setSelection(selection)
            setResult(createSavedPaymentMethodResult(selection, null))
            return
        }

        coroutineScope.launch {
            updateProcessing(true)
            update().fold(
                onSuccess = { response ->
                    selectionHolder.setSelection(selection)
                    setResult(createSavedPaymentMethodResult(selection, response))
                },
                onFailure = { error ->
                    updateProcessing(false)
                    _state.update { it.copy(pendingPaymentMethodId = null) }
                    updateError(error.stripeErrorMessage())
                },
            )
        }
    }

    private fun createSavedPaymentMethodResult(
        selection: PaymentSelection.Saved,
        response: com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse?,
    ): EmbeddedActivityResult.Complete {
        return EmbeddedActivityResult.Complete(
            selection = selection,
            previousNewSelections = selectionHolder.previousNewSelections,
            hasBeenConfirmed = false,
            customerState = customerStateHolder.customer.value,
            checkoutSessionResponse = response,
            shouldInvokeSelectionCallback = true,
            launchMode = launchMode,
        )
    }

    override fun updatePrimaryButton(callback: (PrimaryButton.UIState?) -> PrimaryButton.UIState?) {
        val newUiState = callback(usBankAccountFormPrimaryButtonUiState)
        usBankAccountFormPrimaryButtonUiState = newUiState
        if (newUiState != null) {
            onClickDelegate.set(newUiState.onClick)
            _state.update {
                it.copy(
                    isEnabled = newUiState.enabled,
                    primaryButtonLabel = newUiState.label,
                )
            }
        } else {
            onClickDelegate.clear()
            _state.update {
                it.copy(
                    isEnabled = selectionHolder.selection.value != null,
                    primaryButtonLabel = primaryButtonLabel(paymentMethodMetadata.stripeIntent, configuration),
                )
            }
        }
    }

    private fun SheetActivityStateHolder.State.updateWithConfirmationState(
        state: ConfirmationHandler.State
    ): SheetActivityStateHolder.State {
        return when (state) {
            is ConfirmationHandler.State.Complete -> {
                eventReporter.reportPaymentResult(state.result, selectionHolder.selection.value)
                when (state.result) {
                    is ConfirmationHandler.Result.Succeeded -> copy(
                        processingState = PrimaryButtonProcessingState.Completed,
                        isEnabled = false
                    )
                    is ConfirmationHandler.Result.Failed -> copy(
                        processingState = PrimaryButtonProcessingState.Idle(null),
                        isEnabled = selectionHolder.selection.value != null,
                        isProcessing = false,
                        error = state.result.message
                    )
                    is ConfirmationHandler.Result.Canceled -> copy(
                        processingState = PrimaryButtonProcessingState.Idle(null),
                        isEnabled = selectionHolder.selection.value != null,
                        isProcessing = false,
                        error = null
                    )
                }
            }
            is ConfirmationHandler.State.Confirming -> copy(
                processingState = PrimaryButtonProcessingState.Processing,
                isProcessing = true,
                isEnabled = false,
                error = null
            )
            is ConfirmationHandler.State.Idle -> copy(
                isProcessing = false,
                processingState = PrimaryButtonProcessingState.Idle(null),
                isEnabled = selectionHolder.selection.value != null
            )
        }
    }

    private fun primaryButtonLabel(
        stripeIntent: StripeIntent,
        configuration: EmbeddedPaymentElement.Configuration
    ): ResolvableString {
        val amount = amount(stripeIntent.amount, stripeIntent.currency)
        val label = configuration.primaryButtonLabel
        val isForPaymentIntent = stripeIntent is PaymentIntent
        if (launchMode is EmbeddedLaunchMode.PaymentOptions) {
            return continueButtonLabel(label)
        }
        return when (configuration.formSheetAction) {
            EmbeddedPaymentElement.FormSheetAction.Continue -> continueButtonLabel(label)
            EmbeddedPaymentElement.FormSheetAction.Confirm -> buyButtonLabel(amount, label, isForPaymentIntent)
        }
    }

    private fun amount(amount: Long?, currency: String?): Amount? {
        return if (amount != null && currency != null) Amount(amount, currency) else null
    }
}
