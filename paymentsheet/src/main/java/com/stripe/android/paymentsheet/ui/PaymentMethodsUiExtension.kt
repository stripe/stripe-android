package com.stripe.android.paymentsheet.ui

import android.content.res.Resources
import androidx.annotation.DrawableRes
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardBrand.Unknown
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.paymentdatacollection.ach.TransformToBankIcon

@DrawableRes
internal fun PaymentMethod.getSavedPaymentMethodIcon(forVerticalMode: Boolean = false): Int {
    return when (type) {
        PaymentMethod.Type.Card -> {
            val brand = CardBrand.fromCode(card?.displayBrand).takeIf { it != Unknown } ?: card?.brand
            if(forVerticalMode) { brand?.getCardBrandIconForVerticalMode() } else { brand?.getCardBrandIcon() }
        }
        PaymentMethod.Type.SepaDebit -> R.drawable.stripe_ic_paymentsheet_sepa
        PaymentMethod.Type.USBankAccount -> usBankAccount?.bankName?.let { TransformToBankIcon(it) }
        else -> null
    } ?: R.drawable.stripe_ic_paymentsheet_card_unknown
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
    CardBrand.CartesBancaires -> R.drawable.stripe_ic_paymentsheet_card_cartes_bancaires
    Unknown -> R.drawable.stripe_ic_paymentsheet_card_unknown
}

@DrawableRes
internal fun CardBrand.getCardBrandIconForVerticalMode(): Int = when (this) {
    CardBrand.Visa -> com.stripe.payments.model.R.drawable.stripe_ic_visa
    CardBrand.AmericanExpress -> com.stripe.payments.model.R.drawable.stripe_ic_amex
    CardBrand.Discover -> com.stripe.payments.model.R.drawable.stripe_ic_discover
    CardBrand.JCB -> com.stripe.payments.model.R.drawable.stripe_ic_jcb
    CardBrand.DinersClub -> com.stripe.payments.model.R.drawable.stripe_ic_diners
    CardBrand.MasterCard -> R.drawable.stripe_ic_paymentsheet_card_mastercard
    CardBrand.UnionPay -> R.drawable.stripe_ic_paymentsheet_card_unionpay
    CardBrand.CartesBancaires -> R.drawable.stripe_ic_paymentsheet_card_cartes_bancaires
    Unknown -> R.drawable.stripe_ic_paymentsheet_card_unknown
}

internal fun PaymentMethod.getLabel(resources: Resources): String? = when (type) {
    PaymentMethod.Type.Card -> createCardLabel(resources, card?.last4).takeIf { it.isNotEmpty() }
    PaymentMethod.Type.SepaDebit -> resources.getString(
        R.string.stripe_paymentsheet_payment_method_item_card_number,
        sepaDebit?.last4
    )
    PaymentMethod.Type.USBankAccount -> resources.getString(
        R.string.stripe_paymentsheet_payment_method_item_card_number,
        usBankAccount?.last4
    )
    else -> null
}

internal fun PaymentMethod.getLabelIcon(): Int? = when (type) {
    PaymentMethod.Type.USBankAccount -> R.drawable.stripe_ic_paymentsheet_bank
    else -> null
}

internal fun createCardLabel(resources: Resources, last4: String?): String {
    return last4?.let {
        resources.getString(
            R.string.stripe_paymentsheet_payment_method_item_card_number,
            last4
        )
    }.orEmpty()
}
