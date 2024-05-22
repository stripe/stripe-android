package com.stripe.android.paymentsheet

import com.stripe.android.model.PaymentMethod

// TODO: figure out which package this belongs in
data class DisplayableSavedPaymentMethod(
    val displayName: String,
    val paymentMethod: PaymentMethod,
    val isCbcEligible: Boolean = false
)