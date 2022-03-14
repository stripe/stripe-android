package com.stripe.android

/**
 * Enums of beta headers allowed to be override when initializing [Stripe].
 */
enum class StripeApiBeta(val code: String) {
    WeChatPayV1("wechat_pay_beta=v1"),
    USBankAccount("us_bank_account_beta=v2");
}
