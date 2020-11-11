package com.stripe.android.model

import kotlinx.android.parcel.Parcelize

@Parcelize
data class IssuingCardPin(
    val pin: String
) : StripeModel
