package com.stripe.android.model

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
data class IssuingCardPin(
    val pin: String
) : StripeModel
