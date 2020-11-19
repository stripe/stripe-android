package com.stripe.android.model

import kotlinx.parcelize.Parcelize

@Parcelize
data class IssuingCardPin(
    val pin: String
) : StripeModel
