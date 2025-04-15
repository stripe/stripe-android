package com.stripe.android.paymentelement.confirmation.lpms.foundations.network

import com.stripe.android.model.PaymentMethod

data class SavedCard(
    val customerId: String,
    val paymentMethod: PaymentMethod,
)
