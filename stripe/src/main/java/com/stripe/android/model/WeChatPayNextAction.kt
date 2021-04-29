package com.stripe.android.model

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeModel
import com.stripe.android.model.WeChat
import kotlinx.parcelize.Parcelize

@Parcelize
data class WeChatPayNextAction internal constructor(
    val paymentIntent: PaymentIntent,
    val weChat: WeChat,
) : StripeModel
