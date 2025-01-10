package com.stripe.android.paymentsheet.ui

import androidx.annotation.DrawableRes
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
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
            if (forVerticalMode) { brand?.getCardBrandIconForVerticalMode() } else { brand?.getCardBrandIcon() }
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
internal fun PaymentMethod.getSavedPaymentMethodIconForVerticalMode(): Int {
    return when (type) {
        PaymentMethod.Type.Card -> {
            val brand = CardBrand.fromCode(card?.displayBrand).takeIf { it != Unknown } ?: card?.brand
            brand?.getCardBrandIconForVerticalMode()
        }
        PaymentMethod.Type.SepaDebit -> R.drawable.stripe_ic_paymentsheet_sepa
        PaymentMethod.Type.USBankAccount -> usBankAccount?.bankName?.let { TransformToBankIcon(it) }
        else -> null
    } ?: R.drawable.stripe_ic_paymentsheet_card_unknown
}

@DrawableRes
internal fun CardBrand.getCardBrandIconForVerticalMode(): Int = when (this) {
    CardBrand.Visa -> com.stripe.payments.model.R.drawable.stripe_ic_visa_unpadded
    CardBrand.AmericanExpress -> com.stripe.payments.model.R.drawable.stripe_ic_amex_unpadded
    CardBrand.Discover -> com.stripe.payments.model.R.drawable.stripe_ic_discover_unpadded
    CardBrand.JCB -> com.stripe.payments.model.R.drawable.stripe_ic_jcb_unpadded
    CardBrand.DinersClub -> com.stripe.payments.model.R.drawable.stripe_ic_diners_unpadded
    CardBrand.MasterCard -> com.stripe.payments.model.R.drawable.stripe_ic_mastercard_unpadded
    CardBrand.UnionPay -> com.stripe.payments.model.R.drawable.stripe_ic_unionpay_unpadded
    CardBrand.CartesBancaires -> com.stripe.payments.model.R.drawable.stripe_ic_cartes_bancaires_unpadded
    Unknown -> R.drawable.stripe_ic_paymentsheet_card_unknown
}

@DrawableRes
internal fun getLinkIcon(shouldOverrideSystemTheme: Boolean = false, showLightIcon: Boolean = false): Int {
    if (!shouldOverrideSystemTheme) {
        return R.drawable.stripe_ic_paymentsheet_link_ref
    } else if (showLightIcon) {
        return R.drawable.stripe_ic_paymentsheet_link_light
    } else {
        return R.drawable.stripe_ic_paymentsheet_link_dark
    }
}

internal fun PaymentMethod.getLabel(): ResolvableString? = when (type) {
    PaymentMethod.Type.Card -> createCardLabel(card?.last4)
    PaymentMethod.Type.SepaDebit -> resolvableString(
        R.string.stripe_paymentsheet_payment_method_item_card_number,
        sepaDebit?.last4
    )
    PaymentMethod.Type.USBankAccount -> resolvableString(
        R.string.stripe_paymentsheet_payment_method_item_card_number,
        usBankAccount?.last4
    )
    else -> null
}

internal fun PaymentMethod.getLabelIcon(): Int? = when (type) {
    PaymentMethod.Type.USBankAccount -> R.drawable.stripe_ic_paymentsheet_bank
    else -> null
}

internal fun createCardLabel(last4: String?): ResolvableString? {
    return last4?.let {
        resolvableString(
            R.string.stripe_paymentsheet_payment_method_item_card_number,
            last4
        )
    }
}
