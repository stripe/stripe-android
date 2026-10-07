package com.stripe.android.customersheet

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection

internal fun interface PaymentOptionSelectionFactory {
    fun create(
        selection: PaymentSelection?,
        canUseGooglePay: Boolean,
        appearance: PaymentSheet.Appearance,
    ): PaymentOptionSelection?
}

internal class DefaultPaymentOptionSelectionFactory(
    private val paymentOptionFactory: PaymentOptionFactory,
) : PaymentOptionSelectionFactory {
    override fun create(
        selection: PaymentSelection?,
        canUseGooglePay: Boolean,
        appearance: PaymentSheet.Appearance,
    ): PaymentOptionSelection? {
        return when (selection) {
            is PaymentSelection.GooglePay -> {
                PaymentOptionSelection.GooglePay(
                    paymentOption = paymentOptionFactory.create(
                        selection = selection,
                        linkBrand = null,
                        appearance = appearance,
                    ),
                ).takeIf { canUseGooglePay }
            }
            is PaymentSelection.Saved -> {
                PaymentOptionSelection.PaymentMethod(
                    paymentMethod = selection.paymentMethod,
                    paymentOption = paymentOptionFactory.create(
                        selection = selection,
                        linkBrand = null,
                        appearance = appearance,
                    ),
                )
            }
            else -> null
        }
    }
}
