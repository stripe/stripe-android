package com.stripe.android.model

internal data class Stripe3dsRedirect internal constructor(val url: String) {
    internal companion object {
        private const val FIELD_STRIPE_JS = "stripe_js"

        internal fun create(sdkData: StripeIntent.SdkData): Stripe3dsRedirect {
            require(sdkData.is3ds1) {
                "Expected SdkData with type='three_d_secure_redirect'."
            }

            return Stripe3dsRedirect(
                sdkData.data[FIELD_STRIPE_JS] as String
            )
        }
    }
}
