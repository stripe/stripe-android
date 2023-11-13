package com.stripe.android.paymentsheet.viewmodels

import android.content.Context
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.ui.core.Amount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import com.stripe.android.ui.core.R as StripeUiCoreR

internal class PrimaryButtonUiStateMapper(
    private val context: Context,
    private val config: PaymentSheet.Configuration,
    private val isProcessingPayment: Boolean,
    private val currentScreenFlow: Flow<PaymentSheetScreen>,
    private val buttonsEnabledFlow: Flow<Boolean>,
    private val amountFlow: Flow<Amount?>,
    private val selectionFlow: Flow<PaymentSelection?>,
    private val customPrimaryButtonUiStateFlow: Flow<PrimaryButton.UIState?>,
    private val onClick: () -> Unit,
) {

    fun forCompleteFlow(): Flow<PrimaryButton.UIState?> {
        return combine(
            currentScreenFlow,
            buttonsEnabledFlow,
            amountFlow,
            selectionFlow,
            customPrimaryButtonUiStateFlow,
        ) { screen, buttonsEnabled, amount, selection, customPrimaryButton ->
            customPrimaryButton ?: PrimaryButton.UIState(
                label = buyButtonLabel(amount),
                onClick = onClick,
                enabled = buttonsEnabled && selection != null,
                lockVisible = true,
            ).takeIf { screen.showsBuyButton }
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

    private fun buyButtonLabel(amount: Amount?): String {
        return if (config.primaryButtonLabel != null) {
            config.primaryButtonLabel
        } else if (isProcessingPayment) {
            val fallback = context.getString(R.string.stripe_paymentsheet_pay_button_label)
            amount?.buildPayButtonLabel(context.resources) ?: fallback
        } else {
            context.getString(StripeUiCoreR.string.stripe_setup_button_label)
        }
    }

    private fun continueButtonLabel(): String {
        val customLabel = config.primaryButtonLabel
        return customLabel ?: context.getString(StripeUiCoreR.string.stripe_continue_button_label)
    }
}
