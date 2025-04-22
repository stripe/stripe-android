package com.stripe.android.paymentelement.embedded.form

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.mainthread.MainThreadOnlyMutableStateFlow
import com.stripe.android.core.mainthread.update
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.amount
import com.stripe.android.paymentsheet.model.currency
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import com.stripe.android.paymentsheet.utils.buyButtonLabel
import com.stripe.android.paymentsheet.utils.continueButtonLabel
import com.stripe.android.paymentsheet.utils.reportPaymentResult
import com.stripe.android.ui.core.Amount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

internal interface FormActivityStateHelper {
    val state: StateFlow<State>
    fun updateConfirmationState(confirmationState: ConfirmationHandler.State)
    fun updateMandate(mandateText: ResolvableString?)
    fun updatePrimaryButton(callback: (PrimaryButton.UIState?) -> PrimaryButton.UIState?)
    fun updateError(error: ResolvableString?)

    data class State(
        val primaryButtonLabel: ResolvableString,
        val isEnabled: Boolean,
        val processingState: PrimaryButtonProcessingState,
        val isProcessing: Boolean,
        val shouldDisplayLockIcon: Boolean,
        val error: ResolvableString? = null,
        val mandateText: ResolvableString? = null,
    )
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Singleton
internal class DefaultFormActivityStateHelper @Inject constructor(
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val configuration: EmbeddedPaymentElement.Configuration,
    private val onClickDelegate: OnClickOverrideDelegate,
    private val eventReporter: EventReporter,
    @ViewModelScope coroutineScope: CoroutineScope,
) : FormActivityStateHelper {
    private val _state = MainThreadOnlyMutableStateFlow(
        FormActivityStateHelper.State(
            primaryButtonLabel = primaryButtonLabel(paymentMethodMetadata.stripeIntent, configuration),
            isEnabled = false,
            processingState = PrimaryButtonProcessingState.Idle(null),
            isProcessing = false,
            shouldDisplayLockIcon = configuration.formSheetAction == EmbeddedPaymentElement.FormSheetAction.Confirm,
        )
    )
    override val state: StateFlow<FormActivityStateHelper.State> = _state

    private var usBankAccountFormPrimaryButtonUiState: PrimaryButton.UIState? = null

    init {
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

    override fun updateConfirmationState(confirmationState: ConfirmationHandler.State) {
        _state.update {
            it.updateWithConfirmationState(confirmationState)
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

    private fun FormActivityStateHelper.State.updateWithConfirmationState(
        state: ConfirmationHandler.State
    ): FormActivityStateHelper.State {
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
        return when (configuration.formSheetAction) {
            EmbeddedPaymentElement.FormSheetAction.Continue -> continueButtonLabel(label)
            EmbeddedPaymentElement.FormSheetAction.Confirm -> buyButtonLabel(amount, label, isForPaymentIntent)
        }
    }

    private fun amount(amount: Long?, currency: String?): Amount? {
        return if (amount != null && currency != null) Amount(amount, currency) else null
    }
}
