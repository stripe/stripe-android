package com.stripe.android.paymentsheet.viewmodels

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.SelectSavedPaymentMethods.CvcRecollectionState
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.utils.buyButtonLabel
import com.stripe.android.paymentsheet.utils.combine
import com.stripe.android.paymentsheet.utils.continueButtonLabel
import com.stripe.android.ui.core.Amount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

internal class PrimaryButtonUiStateMapper(
    private val config: PaymentSheet.Configuration,
    private val isProcessingPayment: Boolean,
    private val currentScreenFlow: StateFlow<PaymentSheetScreen>,
    private val buttonsEnabledFlow: StateFlow<Boolean>,
    private val amountFlow: StateFlow<Amount?>,
    private val selectionFlow: StateFlow<PaymentSelection?>,
    private val customPrimaryButtonUiStateFlow: StateFlow<PrimaryButton.UIState?>,
    private val cvcCompleteFlow: StateFlow<Boolean>,
    private val onClick: () -> Unit,
) {

    fun forCompleteFlow(): Flow<PrimaryButton.UIState?> {
        return combine(
            currentScreenFlow,
            buttonsEnabledFlow,
            amountFlow,
            selectionFlow,
            customPrimaryButtonUiStateFlow,
            cvcCompleteFlow
        ) { screen, buttonsEnabled, amount, selection, customPrimaryButton, cvcComplete ->
            screen.buyButtonState.map { buyButtonState ->
                customPrimaryButton ?: PrimaryButton.UIState(
                    label = buyButtonState.buyButtonOverride?.label ?: buyButtonLabel(
                        amount,
                        config.primaryButtonLabel,
                        isProcessingPayment
                    ),
                    onClick = onClick,
                    enabled = buttonsEnabled && selection != null &&
                        cvcRecollectionCompleteOrNotRequired(screen, cvcComplete, selection),
                    lockVisible = buyButtonState.buyButtonOverride?.lockEnabled ?: true,
                ).takeIf { buyButtonState.visible }
            }
        }.flatMapLatest { it }
    }

    fun forCustomFlow(): Flow<PrimaryButton.UIState?> {
        return combine(
            currentScreenFlow,
            buttonsEnabledFlow,
            selectionFlow,
            customPrimaryButtonUiStateFlow,
        ) { screen, buttonsEnabled, selection, customPrimaryButton ->
            customPrimaryButton ?: PrimaryButton.UIState(
                label = continueButtonLabel(config.primaryButtonLabel),
                onClick = onClick,
                enabled = buttonsEnabled && selection != null,
                lockVisible = false,
            ).takeIf {
                screen.showsContinueButton || selection?.requiresConfirmation == true
            }
        }
    }

    private fun cvcRecollectionCompleteOrNotRequired(
        screen: PaymentSheetScreen,
        complete: Boolean,
        selection: PaymentSelection
    ): Boolean {
        return if (
            (screen as? PaymentSheetScreen.SelectSavedPaymentMethods)
                ?.cvcRecollectionState is CvcRecollectionState.Required &&
            (selection as? PaymentSelection.Saved)
                ?.paymentMethod?.type == PaymentMethod.Type.Card
        ) {
            complete
        } else {
            true
        }
    }
}
