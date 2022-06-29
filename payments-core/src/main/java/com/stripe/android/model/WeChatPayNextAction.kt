package com.stripe.android.model

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
data class WeChatPayNextAction internal constructor(
    val paymentIntent: PaymentIntent,
    val weChat: WeChat
) : StripeModel
