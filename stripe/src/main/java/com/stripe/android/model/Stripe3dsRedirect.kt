package com.stripe.android.model

internal data class Stripe3dsRedirect internal constructor(val url: String) {
    internal companion object {
        internal fun create(sdkData: StripeIntent.SdkData): Stripe3dsRedirect {
            require(sdkData.data is StripeIntent.NextActionData.SdkData.Use3DS1) {
                "Expected SdkData with type='three_d_secure_redirect'."
            }

            return Stripe3dsRedirect(sdkData.data.url)
        }
    }
}
