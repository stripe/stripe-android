package com.stripe.android.model

import kotlinx.parcelize.Parcelize

@Parcelize
internal data class PaymentMethodsList(
    val paymentMethods: List<PaymentMethod>
) : StripeModel
