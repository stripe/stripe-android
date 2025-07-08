package com.stripe.android.paymentsheet.ui

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.wallet.label
import com.stripe.android.link.ui.wallet.sublabel
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardBrand.Unknown
import com.stripe.android.model.LinkPaymentDetails
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.paymentdatacollection.ach.TransformToBankIcon
import com.stripe.android.uicore.IconStyle
import com.stripe.android.uicore.LocalIconStyle
import com.stripe.android.ui.core.R as PaymentsUiCoreR

@DrawableRes
internal fun PaymentMethod.getSavedPaymentMethodIcon(
    forVerticalMode: Boolean = false,
    showNightIcon: Boolean? = null,
): Int {
    return when (type) {
        PaymentMethod.Type.Card -> {
            if (isLinkPaymentMethod || isLinkPassthroughMode) {
                // Link card brand or passthrough mode
                getLinkIcon(
                    showNightIcon = showNightIcon,
                    iconOnly = forVerticalMode
                )
            } else {
                card?.getSavedPaymentMethodIcon(
                    forVerticalMode = forVerticalMode,
                    showNightIcon = showNightIcon
                )
            }
        }
        PaymentMethod.Type.SepaDebit -> getSepaIcon(showNightIcon = showNightIcon)
        PaymentMethod.Type.USBankAccount -> {
            if (isLinkPassthroughMode) {
                // Link passthrough mode for US bank account
                getLinkIcon(
                    showNightIcon = showNightIcon,
                    iconOnly = forVerticalMode
                )
            } else {
                TransformToBankIcon(usBankAccount?.bankName)
            }
        }
        PaymentMethod.Type.Link -> getLinkIcon(showNightIcon = showNightIcon, iconOnly = forVerticalMode)
        else -> null
    } ?: R.drawable.stripe_ic_paymentsheet_card_unknown_ref
}

@DrawableRes
internal fun PaymentMethod.Card?.getSavedPaymentMethodIcon(
    forVerticalMode: Boolean = false,
    showNightIcon: Boolean? = null,
): Int {
    val brand = if (this != null) {
        CardBrand.fromCode(displayBrand).takeIf { it != Unknown } ?: brand
    } else {
        Unknown
    }

    return brand.getSavedPaymentMethodIcon(forVerticalMode, showNightIcon)
}

@DrawableRes
private fun CardBrand.getSavedPaymentMethodIcon(
    forVerticalMode: Boolean = false,
    showNightIcon: Boolean? = null,
): Int {
    // Vertical mode icons are the same for light & dark
    return if (forVerticalMode) {
        getCardBrandIconForVerticalMode()
    } else {
        getCardBrandIconForHorizontalMode(
            showNightIcon = showNightIcon,
        )
    }
}

@DrawableRes
internal fun EditCardPayload.getSavedPaymentMethodIcon(
    forVerticalMode: Boolean = false,
    showNightIcon: Boolean? = null,
): Int {
    // Vertical mode icons are the same for light & dark
    return if (forVerticalMode) {
        cardBrand.getCardBrandIconForVerticalMode()
    } else {
        cardBrand.getCardBrandIconForHorizontalMode(
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
    Unknown -> com.stripe.payments.model.R.drawable.stripe_ic_unknown_brand_unpadded
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
internal fun getLinkIcon(
    showNightIcon: Boolean? = null,
    iconOnly: Boolean = false,
): Int {
    if (iconOnly) {
        return R.drawable.stripe_ic_paymentsheet_link_arrow
    }

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

internal fun PaymentMethod.getLabel(canShowSublabel: Boolean = false): ResolvableString? = when (type) {
    PaymentMethod.Type.Card -> {
        if (isLinkPaymentMethod) {
            if (canShowSublabel) {
                linkPaymentDetails?.label
            } else {
                linkPaymentDetails?.sublabel
            }
        } else {
            createCardLabel(card?.last4)
        }
    }
    PaymentMethod.Type.SepaDebit -> resolvableString(
        R.string.stripe_paymentsheet_payment_method_item_card_number,
        sepaDebit?.last4
    )
    PaymentMethod.Type.USBankAccount -> resolvableString(
        R.string.stripe_paymentsheet_payment_method_item_card_number,
        usBankAccount?.last4
    )
    PaymentMethod.Type.Link -> {
        if (canShowSublabel) {
            linkPaymentDetails?.label
        } else {
            linkPaymentDetails?.sublabel
        }
    }
    else -> null
}

@Composable
internal fun PaymentMethod.getLabelIcon(): Int? {
    val iconStyle = LocalIconStyle.current

    val bankIcon = when (iconStyle) {
        IconStyle.Filled -> R.drawable.stripe_ic_paymentsheet_bank
        IconStyle.Outlined -> PaymentsUiCoreR.drawable.stripe_ic_paymentsheet_pm_bank_outlined
    }

    return when (type) {
        PaymentMethod.Type.Card -> {
            when (linkPaymentDetails) {
                is LinkPaymentDetails.BankAccount -> bankIcon
                is LinkPaymentDetails.Card, null -> null
            }
        }
        PaymentMethod.Type.USBankAccount -> bankIcon
        PaymentMethod.Type.Link -> {
            when (linkPaymentDetails) {
                is LinkPaymentDetails.BankAccount -> bankIcon
                is LinkPaymentDetails.Card, null -> null
            }
        }
        else -> null
    }
}

internal val PaymentMethod.shouldTintLabelIcon: Boolean
    get() = type != PaymentMethod.Type.Link && !isLinkPassthroughMode

internal fun createCardLabel(last4: String?): ResolvableString? {
    return last4?.let {
        resolvableString(
            R.string.stripe_paymentsheet_payment_method_item_card_number,
            last4
        )
    }
}
