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
    ): Int? {
        return if (isCompleteFlow) {
            when (screen) {
                is PaymentSheetScreen.SelectSavedPaymentMethods -> {
                    if (isWalletEnabled) {
                        null
                    } else {
                        R.string.stripe_paymentsheet_select_payment_method
                    }
                }
                is PaymentSheetScreen.AddFirstPaymentMethod -> {
                    R.string.stripe_paymentsheet_add_payment_method_title.takeUnless {
                        isWalletEnabled
                    }
                }
                is PaymentSheetScreen.EditPaymentMethod -> {
                    StripeR.string.stripe_title_update_card
                }
                is PaymentSheetScreen.Loading,
                is PaymentSheetScreen.AddAnotherPaymentMethod,
                null -> {
                    null
                }
            }
        } else {
            when (screen) {
                is PaymentSheetScreen.Loading, -> {
                    null
                }
                is PaymentSheetScreen.SelectSavedPaymentMethods -> {
                    R.string.stripe_paymentsheet_select_payment_method
                }
                is PaymentSheetScreen.AddFirstPaymentMethod,
                is PaymentSheetScreen.AddAnotherPaymentMethod -> {
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
        }
    }
}
