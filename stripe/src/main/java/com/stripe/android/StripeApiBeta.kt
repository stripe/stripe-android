package com.stripe.android

enum class StripeApiBeta(val code: String) {
    AlipayV1("alipay_beta=v1"),
    OxxoV1("oxxo_beta=v1");

    internal companion object {
        internal fun fromCode(code: String): StripeApiBeta? {
            return values().firstOrNull { it.code == code }
        }
    }
}
