package com.stripe.android

internal data class StripeUid internal constructor(val value: String) {
    internal companion object {
        @JvmSynthetic
        internal fun create(uid: String): StripeUid {
            return StripeUid(uid)
        }
    }
}
