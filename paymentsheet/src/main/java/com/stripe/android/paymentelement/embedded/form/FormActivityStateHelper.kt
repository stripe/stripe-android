package com.stripe.android.paymentelement.embedded.form

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.model.amount
import com.stripe.android.paymentsheet.model.currency
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import com.stripe.android.paymentsheet.utils.buyButtonLabel
import com.stripe.android.ui.core.Amount
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

internal interface FormActivityStateHelper {
    val state: StateFlow<State>
    fun update(confirmationState: ConfirmationHandler.State)

    data class State(
        val primaryButtonLabel: ResolvableString,
        val isEnabled: Boolean,
        val processingState: PrimaryButtonProcessingState,
        val isProcessing: Boolean,
    )
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Singleton
internal class DefaultFormActivityStateHelper @Inject constructor(
    paymentMethodMetadata: PaymentMethodMetadata,
    selectionHolder: EmbeddedSelectionHolder,
    configuration: EmbeddedPaymentElement.Configuration,
) : FormActivityStateHelper {
    private val primaryButtonProcessingState: MutableStateFlow<PrimaryButtonProcessingState> =
        MutableStateFlow(PrimaryButtonProcessingState.Idle(null))
    private val isProcessing = MutableStateFlow(false)
    private val isEnabled: StateFlow<Boolean> = combineAsStateFlow(
        selectionHolder.selection,
        isProcessing
    ) { selection, processing ->
        selection != null && !processing
    }
    private val primaryButtonLabel = MutableStateFlow(
        primaryButtonLabel(paymentMethodMetadata.stripeIntent, configuration)
    )

    override val state: StateFlow<FormActivityStateHelper.State> = combineAsStateFlow(
        primaryButtonProcessingState,
        isEnabled,
        isProcessing,
        primaryButtonLabel
    ) { buttonProcessingState, enabled, processing, buttonLabel ->
        FormActivityStateHelper.State(
            primaryButtonLabel = buttonLabel,
            isEnabled = enabled,
            processingState = buttonProcessingState,
            isProcessing = processing,
        )
    }

    override fun update(confirmationState: ConfirmationHandler.State) {
        updateUiState(confirmationState)
    }

    private fun updateUiState(confirmationState: ConfirmationHandler.State) {
        when (confirmationState) {
            is ConfirmationHandler.State.Complete -> updateCompleteState(confirmationState)
            is ConfirmationHandler.State.Confirming -> updateConfirmingState()
            is ConfirmationHandler.State.Idle -> updateIdleState()
        }
    }

    private fun updateCompleteState(confirmationState: ConfirmationHandler.State.Complete) {
        primaryButtonProcessingState.value = if (confirmationState.result is ConfirmationHandler.Result.Succeeded) {
            PrimaryButtonProcessingState.Completed
        } else {
            PrimaryButtonProcessingState.Idle(null)
        }
        isProcessing.value = false
    }

    private fun updateConfirmingState() {
        primaryButtonProcessingState.value = PrimaryButtonProcessingState.Processing
        isProcessing.value = true
    }

    private fun updateIdleState() {
        isProcessing.value = false
        primaryButtonProcessingState.value = PrimaryButtonProcessingState.Idle(null)
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
