package com.stripe.android

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeModel
import com.stripe.android.model.WeChat
import kotlinx.parcelize.Parcelize

@Parcelize
data class WechatPayNextAction internal constructor(
    val intent: PaymentIntent,
    val wechat: WeChat,
) : StripeModel
