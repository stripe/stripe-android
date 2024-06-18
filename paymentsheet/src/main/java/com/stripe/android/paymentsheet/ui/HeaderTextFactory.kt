package com.stripe.android.paymentsheet.ui

import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.R as StripeR

internal class HeaderTextFactory(
    private val isCompleteFlow: Boolean,
) {

    fun create(
        screen: PaymentSheetScreen?,
        isWalletEnabled: Boolean,
        types: List<PaymentMethodCode>,
        isEditing: Boolean = false,
    ): Int? {
        return if (isCompleteFlow) {
            createForCompleteFlow(screen, isWalletEnabled, isEditing)
        } else {
            createForFlowController(screen, types, isWalletEnabled, isEditing)
        }
    }

    private fun createForCompleteFlow(
        screen: PaymentSheetScreen?,
        isWalletEnabled: Boolean,
        isEditing: Boolean,
    ) = when (screen) {
        is PaymentSheetScreen.SelectSavedPaymentMethods -> {
            if (isWalletEnabled) {
                null
            } else {
                R.string.stripe_paymentsheet_select_payment_method
            }
        }
        is PaymentSheetScreen.AddFirstPaymentMethod, is PaymentSheetScreen.VerticalMode -> {
            R.string.stripe_paymentsheet_add_payment_method_title.takeUnless {
                isWalletEnabled
            }
        }
        is PaymentSheetScreen.EditPaymentMethod -> {
            StripeR.string.stripe_title_update_card
        }
        is PaymentSheetScreen.ManageOneSavedPaymentMethod -> R.string.stripe_paymentsheet_remove_pm_title
        is PaymentSheetScreen.ManageSavedPaymentMethods -> getHeaderTextForManageScreen(isEditing)
        is PaymentSheetScreen.Loading,
        is PaymentSheetScreen.AddAnotherPaymentMethod,
        is PaymentSheetScreen.Form,
        null -> {
            null
        }
    }

    private fun createForFlowController(
        screen: PaymentSheetScreen?,
        types: List<PaymentMethodCode>,
        isWalletEnabled: Boolean,
        isEditing: Boolean,
    ) = when (screen) {
        is PaymentSheetScreen.Loading, is PaymentSheetScreen.Form -> {
            null
        }
        is PaymentSheetScreen.SelectSavedPaymentMethods -> {
            R.string.stripe_paymentsheet_select_payment_method
        }
        is PaymentSheetScreen.ManageOneSavedPaymentMethod -> R.string.stripe_paymentsheet_remove_pm_title
        is PaymentSheetScreen.ManageSavedPaymentMethods -> getHeaderTextForManageScreen(isEditing)
        is PaymentSheetScreen.AddFirstPaymentMethod,
        is PaymentSheetScreen.AddAnotherPaymentMethod,
        is PaymentSheetScreen.VerticalMode -> {
            val title = if (types.singleOrNull() == PaymentMethod.Type.Card.code) {
                StripeR.string.stripe_title_add_a_card
            } else {
                R.string.stripe_paymentsheet_choose_payment_method
            }

            title.takeUnless { isWalletEnabled }
        }
        is PaymentSheetScreen.EditPaymentMethod -> {
            StripeR.string.stripe_title_update_card
        }
        null -> {
            null
        }
    }

    private fun getHeaderTextForManageScreen(isEditing: Boolean): Int {
        return if (isEditing) {
            R.string.stripe_paymentsheet_manage_payment_methods
        } else {
            R.string.stripe_paymentsheet_select_payment_method
        }
    }
}
