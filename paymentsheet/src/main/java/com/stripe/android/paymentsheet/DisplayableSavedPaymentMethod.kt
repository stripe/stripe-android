package com.stripe.android.paymentsheet

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod

internal data class DisplayableSavedPaymentMethod(
    val displayName: ResolvableString,
    val paymentMethod: PaymentMethod,
    val isCbcEligible: Boolean = false,
) {
    fun isModifiable(): Boolean {
        val hasMultipleNetworks = paymentMethod.card?.networks?.available?.let { available ->
            available.size > 1
        } ?: false

        return isCbcEligible && hasMultipleNetworks
    }

    fun getDescription() = when (paymentMethod.type) {
        PaymentMethod.Type.Card -> {
            resolvableString(
                com.stripe.android.R.string.stripe_card_ending_in,
                brandDisplayName(),
                paymentMethod.card?.last4
            )
        }
        PaymentMethod.Type.SepaDebit -> resolvableString(
            R.string.stripe_bank_account_ending_in,
            paymentMethod.sepaDebit?.last4
        )
        PaymentMethod.Type.USBankAccount -> resolvableString(
            R.string.stripe_bank_account_ending_in,
            paymentMethod.usBankAccount?.last4
        )
        else -> resolvableString("")
    }

    fun getModifyDescription() = resolvableString(
        R.string.stripe_paymentsheet_modify_pm,
        getDescription()
    )

    fun getRemoveDescription(): ResolvableString {
        return resolvableString(
            R.string.stripe_paymentsheet_remove_pm,
            getDescription(),
        )
    }

    fun brandDisplayName(): String? {
        val brand = paymentMethod.card?.displayBrand?.let { CardBrand.fromCode(it) }
            ?: paymentMethod.card?.brand
        return brand?.displayName
    }
}
