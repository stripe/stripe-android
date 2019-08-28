package com.stripe.android

internal data class StripeUid constructor(val value: String) {
    companion object {
        @JvmStatic
        fun create(uid: String): StripeUid {
            return StripeUid(uid)
        }
    }
}
