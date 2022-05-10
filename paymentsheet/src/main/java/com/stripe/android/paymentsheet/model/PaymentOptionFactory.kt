package com.stripe.android.paymentsheet.model

import android.content.res.Resources
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.createCardLabel
import com.stripe.android.paymentsheet.ui.getCardBrandIcon
import com.stripe.android.paymentsheet.ui.getLabel
import com.stripe.android.paymentsheet.ui.getSavedPaymentMethodIcon

internal class PaymentOptionFactory(
    private val resources: Resources
) {
    fun create(selection: PaymentSelection): PaymentOption {
        return when (selection) {
            PaymentSelection.GooglePay -> {
                PaymentOption(
                    drawableResourceId = R.drawable.stripe_google_pay_mark,
                    label = resources.getString(R.string.google_pay)
                )
            }
            is PaymentSelection.Saved -> {
                PaymentOption(
                    drawableResourceId = selection.paymentMethod.getSavedPaymentMethodIcon() ?: 0,
                    label = selection.paymentMethod.getLabel(resources).orEmpty()
                )
            }
            is PaymentSelection.New.Card -> {
                // TODO: Should use labelResource paymentMethodCreateParams or extension function
                PaymentOption(
                    drawableResourceId = selection.brand.getCardBrandIcon(),
                    label = createCardLabel(
                        resources,
                        selection.last4
                    )
                )
            }
            is PaymentSelection.New.Link -> {
                PaymentOption(
                    drawableResourceId = selection.iconResource,
                    label = createCardLabel(
                        resources,
                        selection.label
                    )
                )
            }
            is PaymentSelection.New.GenericPaymentMethod -> {
                PaymentOption(
                    drawableResourceId = selection.iconResource,
                    label = selection.labelResource
                )
            }
        }
    }
}
