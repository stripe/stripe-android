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
internal fun PaymentMethod.getSavedPaymentMethodIcon(
    forVerticalMode: Boolean = false,
    showNightIcon: Boolean? = null,
): Int {
    return when (type) {
        PaymentMethod.Type.Card -> {
            card?.getSavedPaymentMethodIcon(
                forVerticalMode = forVerticalMode,
                showNightIcon = showNightIcon
            )
        }
        PaymentMethod.Type.SepaDebit -> getSepaIcon(showNightIcon = showNightIcon)
        PaymentMethod.Type.USBankAccount -> usBankAccount?.bankName?.let { TransformToBankIcon(it) }
        else -> null
    } ?: R.drawable.stripe_ic_paymentsheet_card_unknown_ref
}

@DrawableRes
internal fun PaymentMethod.Card?.getSavedPaymentMethodIcon(
    forVerticalMode: Boolean = false,
    showNightIcon: Boolean? = null,
): Int {
    this ?: return R.drawable.stripe_ic_paymentsheet_card_unknown_ref
    val brand = CardBrand.fromCode(displayBrand).takeIf { it != Unknown } ?: brand

    // Vertical mode icons are the same for light & dark
    return if (forVerticalMode) {
        brand.getCardBrandIconForVerticalMode()
    } else {
        brand.getCardBrandIconForHorizontalMode(
            showNightIcon = showNightIcon,
        )
    }
}

@DrawableRes
internal fun CardBrand.getCardBrandIcon(): Int {
    return this.getCardBrandIconRef()
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
    Unknown -> R.drawable.stripe_ic_paymentsheet_card_unknown_ref
}

@DrawableRes
internal fun CardBrand.getCardBrandIconForHorizontalMode(
    showNightIcon: Boolean? = null
): Int {
    return getOverridableIcon(
        showNightIcon = showNightIcon,
        systemThemeAwareIconRef = getCardBrandIconRef(),
        nightIcon = getNightIcon(),
        dayIcon = getDayIcon(),
    )
}

@DrawableRes
private fun CardBrand.getCardBrandIconRef(): Int {
    return when (this) {
        CardBrand.Visa -> R.drawable.stripe_ic_paymentsheet_card_visa_ref
        CardBrand.AmericanExpress -> R.drawable.stripe_ic_paymentsheet_card_amex_ref
        CardBrand.Discover -> R.drawable.stripe_ic_paymentsheet_card_discover_ref
        CardBrand.JCB -> R.drawable.stripe_ic_paymentsheet_card_jcb_ref
        CardBrand.DinersClub -> R.drawable.stripe_ic_paymentsheet_card_dinersclub_ref
        CardBrand.MasterCard -> R.drawable.stripe_ic_paymentsheet_card_mastercard_ref
        CardBrand.UnionPay -> R.drawable.stripe_ic_paymentsheet_card_unionpay_ref
        CardBrand.CartesBancaires -> R.drawable.stripe_ic_paymentsheet_card_cartes_bancaires_ref
        Unknown -> R.drawable.stripe_ic_paymentsheet_card_unknown_ref
    }
}

@DrawableRes
private fun CardBrand.getNightIcon(): Int {
    return when (this) {
        CardBrand.Visa -> R.drawable.stripe_ic_paymentsheet_card_visa_night
        CardBrand.AmericanExpress -> R.drawable.stripe_ic_paymentsheet_card_amex_night
        CardBrand.Discover -> R.drawable.stripe_ic_paymentsheet_card_discover_night
        CardBrand.JCB -> R.drawable.stripe_ic_paymentsheet_card_jcb_night
        CardBrand.DinersClub -> R.drawable.stripe_ic_paymentsheet_card_dinersclub_night
        CardBrand.MasterCard -> R.drawable.stripe_ic_paymentsheet_card_mastercard_night
        CardBrand.UnionPay -> R.drawable.stripe_ic_paymentsheet_card_unionpay_night
        CardBrand.CartesBancaires -> R.drawable.stripe_ic_paymentsheet_card_cartes_bancaires_night
        Unknown -> R.drawable.stripe_ic_paymentsheet_card_unknown_night
    }
}

@DrawableRes
private fun CardBrand.getDayIcon(): Int {
    return when (this) {
        CardBrand.Visa -> R.drawable.stripe_ic_paymentsheet_card_visa_day
        CardBrand.AmericanExpress -> R.drawable.stripe_ic_paymentsheet_card_amex_day
        CardBrand.Discover -> R.drawable.stripe_ic_paymentsheet_card_discover_day
        CardBrand.JCB -> R.drawable.stripe_ic_paymentsheet_card_jcb_day
        CardBrand.DinersClub -> R.drawable.stripe_ic_paymentsheet_card_dinersclub_day
        CardBrand.MasterCard -> R.drawable.stripe_ic_paymentsheet_card_mastercard_day
        CardBrand.UnionPay -> R.drawable.stripe_ic_paymentsheet_card_unionpay_day
        CardBrand.CartesBancaires -> R.drawable.stripe_ic_paymentsheet_card_cartes_bancaires_day
        Unknown -> R.drawable.stripe_ic_paymentsheet_card_unknown_day
    }
}

@DrawableRes
internal fun getLinkIcon(showNightIcon: Boolean? = null): Int {
    return getOverridableIcon(
        showNightIcon = showNightIcon,
        systemThemeAwareIconRef = R.drawable.stripe_ic_paymentsheet_link_ref,
        nightIcon = R.drawable.stripe_ic_paymentsheet_link_night,
        dayIcon = R.drawable.stripe_ic_paymentsheet_link_day
    )
}

@DrawableRes
internal fun getSepaIcon(showNightIcon: Boolean? = null): Int {
    return getOverridableIcon(
        showNightIcon = showNightIcon,
        systemThemeAwareIconRef = R.drawable.stripe_ic_paymentsheet_sepa_ref,
        nightIcon = R.drawable.stripe_ic_paymentsheet_sepa_night,
        dayIcon = R.drawable.stripe_ic_paymentsheet_sepa_day
    )
}

// If you don't want to override the system theme, then leave showNightIcon null.
@DrawableRes
private fun getOverridableIcon(
    showNightIcon: Boolean?,
    @DrawableRes systemThemeAwareIconRef: Int,
    @DrawableRes nightIcon: Int,
    @DrawableRes dayIcon: Int
): Int {
    if (showNightIcon == null) {
        return systemThemeAwareIconRef
    } else if (showNightIcon) {
        return nightIcon
    } else {
        return dayIcon
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
