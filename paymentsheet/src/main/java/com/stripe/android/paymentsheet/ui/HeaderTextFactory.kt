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
        screen: PaymentSheetScreen,
        isWalletEnabled: Boolean,
        types: List<PaymentMethodCode>,
    ): Int? {
        return if (isCompleteFlow) {
            when (screen) {
                PaymentSheetScreen.SelectSavedPaymentMethods -> {
                    if (isWalletEnabled) {
                        null
                    } else {
                        R.string.stripe_paymentsheet_select_payment_method
                    }
                }
                PaymentSheetScreen.AddFirstPaymentMethod -> {
                    R.string.stripe_paymentsheet_add_payment_method_title.takeUnless {
                        isWalletEnabled
                    }
                }
                PaymentSheetScreen.Loading,
                PaymentSheetScreen.AddAnotherPaymentMethod -> {
                    null
                }
            }
        } else {
            when (screen) {
                PaymentSheetScreen.Loading, -> {
                    null
                }
                PaymentSheetScreen.SelectSavedPaymentMethods -> {
                    R.string.stripe_paymentsheet_select_payment_method
                }
                PaymentSheetScreen.AddFirstPaymentMethod,
                PaymentSheetScreen.AddAnotherPaymentMethod -> {
                    if (types.singleOrNull() == PaymentMethod.Type.Card.code) {
                        StripeR.string.stripe_title_add_a_card
                    } else {
                        R.string.stripe_paymentsheet_choose_payment_method
                    }
                }
            }
        }
    }
}
