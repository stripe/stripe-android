package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
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
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.addresselement.PaymentElementAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.addresselement.computeBillingEditDistance
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.amount
import com.stripe.android.paymentsheet.model.billingDetails
import com.stripe.android.paymentsheet.model.currency
import com.stripe.android.paymentsheet.model.isLink
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import com.stripe.android.paymentsheet.utils.buyButtonLabel
import com.stripe.android.paymentsheet.utils.continueButtonLabel
import com.stripe.android.paymentsheet.utils.reportPaymentResult
import com.stripe.android.ui.core.Amount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

internal interface SheetActivityStateHolder {
    val state: StateFlow<State>
    val result: SharedFlow<EmbeddedActivityResult>
    val validationRequested: SharedFlow<Unit>
    fun updateMandate(mandateText: ResolvableString?)
    fun updatePrimaryButton(callback: (PrimaryButton.UIState?) -> PrimaryButton.UIState?)
    fun updateError(error: ResolvableString?)
    fun requestValidation()

    fun setResult(result: EmbeddedActivityResult)

    fun updateSavedPaymentSelectionToConfirm(selection: PaymentSelection.Saved?)

    data class State(
        val primaryButtonLabel: ResolvableString,
        val isEnabled: Boolean,
        val processingState: PrimaryButtonProcessingState,
        val isProcessing: Boolean,
        val shouldDisplayLockIcon: Boolean,
        val error: ResolvableString? = null,
        val mandateText: ResolvableString? = null,
        val savedPaymentSelectionToConfirm: PaymentSelection.Saved? = null,
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
    private val linkHandler: LinkHandler,
    private val eventReporterMode: EventReporter.Mode,
    private val autocompleteAddressInteractorFactory: PaymentElementAutocompleteAddressInteractor.Factory,
    private val savedStateHandle: SavedStateHandle,
) : SheetActivityStateHolder {
    private val _state = MutableStateFlow(
        SheetActivityStateHolder.State(
            primaryButtonLabel = primaryButtonLabel(paymentMethodMetadata.stripeIntent, configuration),
            isEnabled = false,
            processingState = PrimaryButtonProcessingState.Idle(null),
            isProcessing = false,
            shouldDisplayLockIcon = launchMode !is EmbeddedLaunchMode.PaymentOptions &&
                configuration.formSheetAction == EmbeddedPaymentElement.FormSheetAction.Confirm,
            savedPaymentSelectionToConfirm = null,
        )
    )
    override val state: StateFlow<SheetActivityStateHolder.State> = _state

    private val _result = MutableSharedFlow<EmbeddedActivityResult>()
    override val result: SharedFlow<EmbeddedActivityResult> = _result

    private val _validationRequested = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val validationRequested: SharedFlow<Unit> = _validationRequested

    private var usBankAccountFormPrimaryButtonUiState: PrimaryButton.UIState? = null
    private var confirmationSelection: PaymentSelection?
        get() = savedStateHandle[IN_PROGRESS_SELECTION_KEY]
        set(value) {
            savedStateHandle[IN_PROGRESS_SELECTION_KEY] = value
        }

    init {
        if (confirmationHandler.state.value is ConfirmationHandler.State.Idle) {
            confirmationSelection = null
        }

        coroutineScope.launch {
            tapToAddHelper.nextStep.collect { result ->
                val formResult = when (result) {
                    is TapToAddNextStep.ConfirmSavedPaymentMethod -> {
                        updateSavedPaymentSelectionToConfirm(
                            result.paymentSelection,
                        )
                        null
                    }
                    is TapToAddNextStep.ShowSavedPaymentMethods -> {
                        if (launchMode is EmbeddedLaunchMode.Complete) {
                            updateSavedPaymentSelectionToConfirm(result.paymentSelection)
                            null
                        } else {
                            EmbeddedActivityResult.Complete(
                                selection = result.paymentSelection,
                                previousNewSelections = selectionHolder.previousNewSelections,
                                hasBeenConfirmed = false,
                                customerState = customerStateHolder.customer.value,
                                shouldInvokeSelectionCallback = false,
                                launchMode = launchMode,
                            )
                        }
                    }
                    TapToAddNextStep.Complete -> {
                        EmbeddedActivityResult.Complete(
                            selection = null,
                            previousNewSelections = selectionHolder.previousNewSelections,
                            hasBeenConfirmed = true,
                            customerState = customerStateHolder.customer.value,
                            shouldInvokeSelectionCallback = false,
                            launchMode = launchMode,
                        )
                    }
                    is TapToAddNextStep.Continue -> {
                        customerStateHolder.addPaymentMethod(result.paymentSelection.paymentMethod)
                        EmbeddedActivityResult.Complete(
                            selection = result.paymentSelection,
                            previousNewSelections = selectionHolder.previousNewSelections,
                            hasBeenConfirmed = false,
                            customerState = customerStateHolder.customer.value,
                            shouldInvokeSelectionCallback = false,
                            launchMode = launchMode,
                        )
                    }
                }
                formResult?.let {
                    setResult(it)
                }
            }
        }

        coroutineScope.launch {
            confirmationHandler.state.collectLatest { confirmationState ->
                if (confirmationState is ConfirmationHandler.State.Confirming && confirmationSelection == null) {
                    confirmationSelection = selectionHolder.selection.value
                }
                val selection = confirmationSelection ?: selectionHolder.selection.value
                if (confirmationState is ConfirmationHandler.State.Complete) {
                    eventReporter.reportPaymentResult(confirmationState.result, selection)
                }
                _state.update {
                    it.updateWithConfirmationState(confirmationState)
                }
                if ((confirmationState as? ConfirmationHandler.State.Complete)?.result
                    is ConfirmationHandler.Result.Succeeded
                ) {
                    reportBillingAddressCompleted(selection)
                    if (eventReporterMode == EventReporter.Mode.Complete && selection?.isLink == true) {
                        linkHandler.setupLink(paymentMethodMetadata.linkState)
                        linkHandler.logOut()
                    }
                    setResult(
                        EmbeddedActivityResult.Complete(
                            selection = null,
                            previousNewSelections = selectionHolder.previousNewSelections,
                            hasBeenConfirmed = true,
                            customerState = customerStateHolder.customer.value,
                            shouldInvokeSelectionCallback = false,
                            launchMode = launchMode,
                        )
                    )
                }
                if (confirmationState is ConfirmationHandler.State.Complete) {
                    confirmationSelection = null
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

    internal fun setInProgressSelection(selection: PaymentSelection) {
        confirmationSelection = selection
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

    override fun requestValidation() {
        _validationRequested.tryEmit(Unit)
        coroutineScope.launch {
            // The disabled overlay can be clicked before the US bank form's LaunchedEffect
            // publishes its custom button state. Wait briefly for that state without delaying
            // validation for forms that collect validationRequested above.
            repeat(VALIDATION_STATE_RETRY_COUNT) {
                val uiState = usBankAccountFormPrimaryButtonUiState
                if (uiState != null) {
                    uiState.onDisabledClick()
                    return@launch
                }
                delay(VALIDATION_STATE_RETRY_DELAY_MILLIS)
            }
        }
    }

    override fun updateSavedPaymentSelectionToConfirm(selection: PaymentSelection.Saved?) {
        _state.update {
            it.copy(savedPaymentSelectionToConfirm = selection)
        }
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
                when (state.result) {
                    is ConfirmationHandler.Result.Succeeded -> copy(
                        processingState = PrimaryButtonProcessingState.Completed,
                        isEnabled = false,
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

    private fun reportBillingAddressCompleted(selection: PaymentSelection?) {
        if (eventReporterMode != EventReporter.Mode.Complete || selection !is PaymentSelection.New) return
        val billingAddress = selection.billingDetails?.address ?: return
        val countryCode = billingAddress.country ?: return
        val autocompleteFilledAddress = autocompleteAddressInteractorFactory.autocompleteFilledAddress
        eventReporter.onBillingAddressCompleted(
            addressCountryCode = countryCode,
            autocompleteResultSelected = autocompleteFilledAddress != null,
            editDistance = autocompleteFilledAddress?.let {
                computeBillingEditDistance(it, billingAddress)
            },
        )
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

    private companion object {
        const val IN_PROGRESS_SELECTION_KEY = "SHEET_ACTIVITY_IN_PROGRESS_PAYMENT_SELECTION"
        const val VALIDATION_STATE_RETRY_COUNT = 32
        const val VALIDATION_STATE_RETRY_DELAY_MILLIS = 16L
    }
}
