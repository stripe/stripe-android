package com.stripe.android.model

import kotlinx.android.parcel.Parcelize

@Parcelize
internal data class PaymentMethodsList(
    val paymentMethods: List<PaymentMethod>
) : StripeModel
