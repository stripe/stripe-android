package com.stripe.android.paymentsheet

import com.stripe.android.model.PaymentMethod

internal data class DisplayableSavedPaymentMethod(
    val displayName: String,
    val paymentMethod: PaymentMethod,
    val isCbcEligible: Boolean = false,
) {
    fun isModifiable(): Boolean {
        val hasMultipleNetworks = paymentMethod.card?.networks?.available?.let { available ->
            available.size > 1
        } ?: false

        return isCbcEligible && hasMultipleNetworks
    }
}
