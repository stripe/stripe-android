package com.stripe.android.paymentsheet.model

import com.stripe.android.R
import com.stripe.android.model.PaymentMethod

internal class PaymentOptionFactory {
    fun create(selection: PaymentSelection): PaymentOption? {
        return when (selection) {
            PaymentSelection.GooglePay -> {
                // TODO(mshafrir-stripe): update values
                PaymentOption(
                    drawableResourceId = R.drawable.stripe_google_pay_mark,
                    label = "Google Pay"
                )
            }
            is PaymentSelection.Saved -> {
                when (selection.paymentMethod.type) {
                    PaymentMethod.Type.Card -> {
                        val brand = selection.paymentMethod.card?.brand
                        brand?.let {
                            PaymentOption(
                                drawableResourceId = it.icon,
                                label = it.displayName
                            )
                        }
                    }
                    else -> {
                        // TODO(mshafrir-stripe): handle other types
                        null
                    }
                }
            }
            is PaymentSelection.New -> {
                // TODO(mshafrir-stripe): handle params
                null
            }
        }
    }
}
