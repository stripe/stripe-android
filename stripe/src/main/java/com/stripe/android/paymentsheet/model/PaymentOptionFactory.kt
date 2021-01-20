package com.stripe.android.paymentsheet.model

import com.stripe.android.R
import com.stripe.android.model.CardBrand
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
            is PaymentSelection.New.Card -> {
                val brand = selection.brand
                PaymentOption(
                    drawableResourceId = when (brand) {
                        CardBrand.Visa -> R.drawable.stripe_ic_paymentsheet_card_visa
                        CardBrand.AmericanExpress -> R.drawable.stripe_ic_paymentsheet_card_amex
                        CardBrand.Discover -> R.drawable.stripe_ic_paymentsheet_card_discover
                        CardBrand.JCB -> R.drawable.stripe_ic_paymentsheet_card_jcb
                        CardBrand.DinersClub -> R.drawable.stripe_ic_paymentsheet_card_dinersclub
                        CardBrand.MasterCard -> R.drawable.stripe_ic_paymentsheet_card_mastercard
                        CardBrand.UnionPay -> R.drawable.stripe_ic_paymentsheet_card_unionpay
                        CardBrand.Unknown -> R.drawable.stripe_ic_paymentsheet_card_unknown
                    },
                    label = brand.displayName
                )
            }
        }
    }
}
