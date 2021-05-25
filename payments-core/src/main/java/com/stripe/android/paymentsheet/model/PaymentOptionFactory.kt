package com.stripe.android.paymentsheet.model

import android.content.res.Resources
import com.stripe.android.R
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod

internal class PaymentOptionFactory(
    private val resources: Resources
) {
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
                        selection.paymentMethod.card?.let { card ->
                            PaymentOption(
                                drawableResourceId = getIcon(card.brand),
                                label = createCardLabel(card.last4)
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
                    drawableResourceId = getIcon(brand),
                    label = createCardLabel(selection.paymentMethodCreateParams.card?.last4)
                )
            }
        }
    }

    private fun createCardLabel(last4: String?): String {
        return last4?.let {
            resources.getString(
                R.string.paymentsheet_payment_method_item_card_number,
                last4
            )
        }.orEmpty()
    }

    private fun getIcon(brand: CardBrand) = when (brand) {
        CardBrand.Visa -> R.drawable.stripe_ic_paymentsheet_card_visa
        CardBrand.AmericanExpress -> R.drawable.stripe_ic_paymentsheet_card_amex
        CardBrand.Discover -> R.drawable.stripe_ic_paymentsheet_card_discover
        CardBrand.JCB -> R.drawable.stripe_ic_paymentsheet_card_jcb
        CardBrand.DinersClub -> R.drawable.stripe_ic_paymentsheet_card_dinersclub
        CardBrand.MasterCard -> R.drawable.stripe_ic_paymentsheet_card_mastercard
        CardBrand.UnionPay -> R.drawable.stripe_ic_paymentsheet_card_unionpay
        CardBrand.Unknown -> R.drawable.stripe_ic_paymentsheet_card_unknown
    }
}
