package com.stripe.android.model

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

/**
 * [WeChat Pay Payments with Sources](https://stripe.com/docs/sources/wechat-pay)
 *
 * [WeChat Pay Payments with PaymentIntents](https://stripe.com/docs/payments/wechat-pay/accept-a-payment?platform=android)
 */
@Parcelize
data class WeChat constructor(
    val statementDescriptor: String? = null,
    val appId: String?,
    val nonce: String?,
    val packageValue: String?,
    val partnerId: String?,
    val prepayId: String?,
    val sign: String?,
    val timestamp: String?,
    val qrCodeUrl: String? = null
) : StripeModel
