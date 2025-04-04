package com.stripe.android.paymentsheet.viewmodels

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.SelectSavedPaymentMethods.CvcRecollectionState
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.utils.buyButtonLabel
import com.stripe.android.paymentsheet.utils.continueButtonLabel
import com.stripe.android.ui.core.Amount
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.flatMapLatestAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow

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

    fun forCompleteFlow(): StateFlow<PrimaryButton.UIState?> {
        return combineAsStateFlow(
            currentScreenFlow,
            buttonsEnabledFlow,
            amountFlow,
            selectionFlow,
            customPrimaryButtonUiStateFlow,
            cvcCompleteFlow
        ) { screen, buttonsEnabled, amount, selection, customPrimaryButton, cvcComplete ->
            screen.buyButtonState.mapAsStateFlow { buyButtonState ->
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
        }.flatMapLatestAsStateFlow { it }
    }

    fun forCustomFlow(): StateFlow<PrimaryButton.UIState?> {
        return combineAsStateFlow(
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
                /**
                 * PaymentMethods requireConfirmation when they have mandates / terms of service
                 * that must be shown to buyers
                 * Check which ones require confirmation here [PaymentSelection.Saved.mandateText]
                 * The continue button is required to obtain the buyers implicit consent on screens
                 * where mandates are shown.
                 */
                val needsUserConsentForSelectedPaymentMethodWithMandate =
                    selection?.requiresConfirmation == true && screen.showsMandates
                screen.showsContinueButton || needsUserConsentForSelectedPaymentMethodWithMandate
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
