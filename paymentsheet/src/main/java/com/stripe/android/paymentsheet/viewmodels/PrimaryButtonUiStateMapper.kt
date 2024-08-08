package com.stripe.android.paymentsheet.viewmodels

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.SelectSavedPaymentMethods.CvcRecollectionState
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.utils.combine
import com.stripe.android.ui.core.Amount
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import com.stripe.android.ui.core.R as StripeUiCoreR

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
            cvcCompleteFlow,
        ) { screen, buttonsEnabled, amount, selection, customPrimaryButton, cvcComplete ->
            customPrimaryButton ?: PrimaryButton.UIState(
                label = buyButtonLabel(amount, screen.buyButtonState.buyButtonOverride?.label),
                onClick = onClick,
                enabled = buttonsEnabled && selection != null &&
                    cvcRecollectionCompleteOrNotRequired(screen, cvcComplete, selection),
                lockVisible = screen.buyButtonState.buyButtonOverride?.lockEnabled ?: true,
            ).takeIf { screen.buyButtonState.visible }
        }
    }

    fun forCustomFlow(): Flow<PrimaryButton.UIState?> {
        return combine(
            currentScreenFlow,
            buttonsEnabledFlow,
            selectionFlow,
            customPrimaryButtonUiStateFlow,
        ) { screen, buttonsEnabled, selection, customPrimaryButton ->
            customPrimaryButton ?: PrimaryButton.UIState(
                label = continueButtonLabel(),
                onClick = onClick,
                enabled = buttonsEnabled && selection != null,
                lockVisible = false,
            ).takeIf {
                screen.showsContinueButton || selection?.requiresConfirmation == true
            }
        }
    }

    private fun buyButtonLabel(amount: Amount?, baseText: ResolvableString?): ResolvableString {
        return config.primaryButtonLabel?.let {
            it.resolvableString
        } ?: run {
            if (isProcessingPayment) {
                val fallback = R.string.stripe_paymentsheet_pay_button_label.resolvableString
                baseText ?: amount?.buildPayButtonLabel() ?: fallback
            } else {
                StripeUiCoreR.string.stripe_setup_button_label.resolvableString
            }
        }
    }

    private fun continueButtonLabel(): ResolvableString {
        return config.primaryButtonLabel?.resolvableString
            ?: StripeUiCoreR.string.stripe_continue_button_label.resolvableString
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
