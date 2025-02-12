package com.stripe.android.paymentelement.embedded.form

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.model.MandateText
import com.stripe.android.paymentsheet.model.amount
import com.stripe.android.paymentsheet.model.currency
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import com.stripe.android.paymentsheet.utils.buyButtonLabel
import com.stripe.android.ui.core.Amount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
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
        val error: ResolvableString? = null,
        val mandateText: MandateText? = null,
        val onClick: (() -> Unit)? = null
    )
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Singleton
internal class DefaultFormActivityStateHelper @Inject constructor(
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val configuration: EmbeddedPaymentElement.Configuration,
    @ViewModelScope coroutineScope: CoroutineScope,
) : FormActivityStateHelper {
    private val _state = MutableStateFlow(
        FormActivityStateHelper.State(
            primaryButtonLabel = primaryButtonLabel(paymentMethodMetadata.stripeIntent, configuration),
            isEnabled = false,
            processingState = PrimaryButtonProcessingState.Idle(null),
            isProcessing = false
        )
    )
    override val state: StateFlow<FormActivityStateHelper.State> = _state

    init {
        coroutineScope.launch {
            selectionHolder.selection.collectLatest { selection ->
                _state.update { currentState ->
                    currentState.copy(
                        isEnabled = selection != null && !currentState.isProcessing
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
        val mandate = if (mandateText != null) {
            MandateText(
                text = mandateText,
                showAbovePrimaryButton = true
            )
        } else {
            null
        }
        _state.update {
            it.copy(
                mandateText = mandate
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
        val currentFormState = _state.value
        val newUiState = callback(
            PrimaryButton.UIState(
                label = currentFormState.primaryButtonLabel,
                onClick = currentFormState.onClick ?: {},
                enabled = currentFormState.isEnabled,
                lockVisible = true
            )
        )
        if (newUiState != null) {
            _state.update {
                it.copy(
                    isEnabled = newUiState.enabled,
                    primaryButtonLabel = newUiState.label,
                    onClick = newUiState.onClick
                )
            }
        } else {
            _state.update {
                it.copy(
                    isEnabled = selectionHolder.selection.value != null,
                    primaryButtonLabel = primaryButtonLabel(paymentMethodMetadata.stripeIntent, configuration),
                    onClick = null
                )
            }
        }
    }

    private fun FormActivityStateHelper.State.updateWithConfirmationState(
        state: ConfirmationHandler.State
    ): FormActivityStateHelper.State {
        return when (state) {
            is ConfirmationHandler.State.Complete -> {
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
        return buyButtonLabel(amount, label, isForPaymentIntent)
    }

    private fun amount(amount: Long?, currency: String?): Amount? {
        return if (amount != null && currency != null) Amount(amount, currency) else null
    }
}
