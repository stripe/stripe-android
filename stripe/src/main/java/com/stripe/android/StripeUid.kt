package com.stripe.android

internal data class StripeUid private constructor(val value: String) {
    companion object {
        @JvmStatic
        fun create(uid: String): StripeUid {
            return StripeUid(uid)
        }
    }
}
