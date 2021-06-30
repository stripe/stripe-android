package com.stripe.android.paymentsheet.model

import android.content.res.Resources
import com.stripe.android.R

internal class PaymentOptionFactory(
    private val resources: Resources
) {
    fun create(selection: PaymentSelection): PaymentOption? {
        return when (selection) {
            PaymentSelection.GooglePay -> {
                PaymentOption(
                    drawableResourceId = R.drawable.stripe_google_pay_mark,
                    label = resources.getString(R.string.google_pay)
                )
            }
            is PaymentSelection.Saved -> {
                return if (SupportedSavedPaymentMethod.isSupported(selection.paymentMethod)) {
                    PaymentOption(
                        drawableResourceId = selection.paymentMethod.getIcon() ?: 0,
                        label = selection.paymentMethod.getLabel(resources).orEmpty()
                    )
                } else {
                    null
                }
            }
            is PaymentSelection.New.Card -> {
                PaymentOption(
                    drawableResourceId = selection.brand.getIcon(),
                    label = createCardLabel(
                        resources,
                        selection.paymentMethodCreateParams.card?.last4
                    )
                )
            }
            is PaymentSelection.New.GenericPaymentMethod -> {
                PaymentOption(
                    drawableResourceId = selection.iconResource,
                    label = resources.getString(selection.labelResource)
                )
            }
        }
    }
}
