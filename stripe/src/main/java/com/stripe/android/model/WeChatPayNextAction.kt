package com.stripe.android.model

import kotlinx.parcelize.Parcelize

@Parcelize
data class WeChatPayNextAction internal constructor(
    val paymentIntent: PaymentIntent,
    val weChat: WeChat,
) : StripeModel
