package com.stripe.android

import androidx.annotation.RestrictTo

/**
 * Enums of beta headers allowed to be override when initializing [Stripe].
 */
enum class StripeApiBeta(val code: String) {
    WeChatPayV1("wechat_pay_beta=v1"),
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) USBankAccount("us_bank_account_beta=v2");
}
