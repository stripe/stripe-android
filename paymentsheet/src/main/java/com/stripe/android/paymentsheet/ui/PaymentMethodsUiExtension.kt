package com.stripe.android.paymentsheet.ui

import android.content.res.Resources
import androidx.annotation.DrawableRes
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.R

@DrawableRes
internal fun PaymentMethod.getSavedPaymentMethodIcon(): Int? = when (type) {
    PaymentMethod.Type.Card -> card?.brand?.getCardBrandIcon()
        ?: R.drawable.stripe_ic_paymentsheet_card_unknown
    PaymentMethod.Type.SepaDebit -> R.drawable.stripe_ic_paymentsheet_pm_sepa_debit
    else -> null
}

@DrawableRes
internal fun CardBrand.getCardBrandIcon(): Int = when (this) {
    CardBrand.Visa -> R.drawable.stripe_ic_paymentsheet_card_visa
    CardBrand.AmericanExpress -> R.drawable.stripe_ic_paymentsheet_card_amex
    CardBrand.Discover -> R.drawable.stripe_ic_paymentsheet_card_discover
    CardBrand.JCB -> R.drawable.stripe_ic_paymentsheet_card_jcb
    CardBrand.DinersClub -> R.drawable.stripe_ic_paymentsheet_card_dinersclub
    CardBrand.MasterCard -> R.drawable.stripe_ic_paymentsheet_card_mastercard
    CardBrand.UnionPay -> R.drawable.stripe_ic_paymentsheet_card_unionpay
    CardBrand.Unknown -> R.drawable.stripe_ic_paymentsheet_card_unknown
}

internal fun PaymentMethod.getLabel(resources: Resources): String? = when (type) {
    PaymentMethod.Type.Card -> createCardLabel(resources, card?.last4)
    PaymentMethod.Type.SepaDebit -> resources.getString(
        R.string.paymentsheet_payment_method_item_card_number,
        sepaDebit?.last4
    )
    else -> null
}

internal fun createCardLabel(resources: Resources, last4: String?): String {
    return last4?.let {
        resources.getString(
            R.string.paymentsheet_payment_method_item_card_number,
            last4
        )
    }.orEmpty()
}
