package com.stripe.android.paymentsheet

import android.content.res.Resources
import com.stripe.android.core.strings.ResolvableString
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

    fun getDescription(resources: Resources) = when (paymentMethod.type) {
        PaymentMethod.Type.Card -> resources.getString(
            com.stripe.android.R.string.stripe_card_ending_in,
            paymentMethod.card?.brand,
            paymentMethod.card?.last4
        )
        PaymentMethod.Type.SepaDebit -> resources.getString(
            R.string.stripe_bank_account_ending_in,
            paymentMethod.sepaDebit?.last4
        )
        PaymentMethod.Type.USBankAccount -> resources.getString(
            R.string.stripe_bank_account_ending_in,
            paymentMethod.usBankAccount?.last4
        )
        else -> ""
    }
}
