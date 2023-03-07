package com.stripe.android.paymentsheet.viewmodels

import android.content.Context
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.ui.core.Amount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class PrimaryButtonUiStateMapper(
    private val context: Context,
    private val config: PaymentSheet.Configuration?,
    private val isProcessingPayment: Boolean,
    private val currentScreenFlow: Flow<PaymentSheetScreen>,
    private val processingStateFlow: Flow<PaymentSheetViewState>,
    private val buttonsEnabledFlow: Flow<Boolean>,
    private val amountFlow: Flow<Amount?>,
    private val selectionFlow: Flow<PaymentSelection?>,
    private val customPrimaryButtonUiStateFlow: Flow<PrimaryButton.UIState?>,
    private val onClick: () -> Unit,
) {

    private val processingLabel: String
        get() = context.getString(R.string.stripe_paymentsheet_primary_button_processing)

    fun forCompleteFlow(): Flow<PrimaryButton.UIState?> {
        return combine(
            currentScreenFlow,
            processingStateFlow,
            buttonsEnabledFlow,
            amountFlow,
            selectionFlow,
            customPrimaryButtonUiStateFlow,
        ) { items ->
            val screen = items[0] as PaymentSheetScreen
            val processingState = items[1] as PaymentSheetViewState
            val buttonsEnabled = items[2] as Boolean
            val amount = items[3] as? Amount
            val selection = items[4] as? PaymentSelection
            val customPrimaryButton = items[5] as? PrimaryButton.UIState

            customPrimaryButton ?: PrimaryButton.UIState(
                processingState = processingState.convert(),
                label = buyButtonLabel(amount, processingState),
                onClick = onClick,
                enabled = buttonsEnabled && selection != null,
                lockVisible = true,
                color = config?.primaryButtonColor,
            ).takeIf { screen.showsBuyButton }
        }
    }

    fun forCustomFlow(): Flow<PrimaryButton.UIState?> {
        return combine(
            currentScreenFlow,
            processingStateFlow,
            buttonsEnabledFlow,
            selectionFlow,
            customPrimaryButtonUiStateFlow,
        ) { screen, processingState, buttonsEnabled, selection, customPrimaryButton ->
            customPrimaryButton ?: PrimaryButton.UIState(
                processingState = processingState.convert(),
                label = continueButtonLabel(processingState),
                onClick = onClick,
                enabled = buttonsEnabled && selection != null,
                lockVisible = false,
                color = config?.primaryButtonColor,
            ).takeIf {
                screen.showsContinueButton || selection?.requiresConfirmation == true
            }
        }
    }

    private fun buyButtonLabel(
        amount: Amount?,
        processingState: PaymentSheetViewState,
    ): String {
        return if (processingState.isProcessing) {
            processingLabel
        } else if (config?.primaryButtonLabel != null) {
            config.primaryButtonLabel
        } else if (isProcessingPayment) {
            val fallback = context.getString(R.string.stripe_paymentsheet_pay_button_label)
            amount?.buildPayButtonLabel(context.resources) ?: fallback
        } else {
            context.getString(R.string.stripe_setup_button_label)
        }
    }

    private fun continueButtonLabel(
        processingState: PaymentSheetViewState,
    ): String {
        return processingLabel.takeIf { processingState.isProcessing }
            ?: config?.primaryButtonLabel
            ?: context.getString(R.string.stripe_continue_button_label)
    }
}

private val PaymentSheetViewState.isProcessing: Boolean
    get() = this is PaymentSheetViewState.StartProcessing

internal fun PaymentSheetViewState.convert(): PrimaryButton.State {
    return when (this) {
        is PaymentSheetViewState.Reset -> {
            PrimaryButton.State.Ready
        }
        is PaymentSheetViewState.StartProcessing -> {
            PrimaryButton.State.StartProcessing
        }
        is PaymentSheetViewState.FinishProcessing -> {
            PrimaryButton.State.FinishProcessing(this.onComplete)
        }
    }
}
