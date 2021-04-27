package com.stripe.android

/**
 * Enums of beta headers allowed to be override when initializing [Stripe].
 */
enum class StripeApiBeta(val code: String) {
    @Deprecated("alipay is public")
    AlipayV1("alipay_beta=v1"),
    WechatPayV1("wechat_pay_beta=v1");
}
