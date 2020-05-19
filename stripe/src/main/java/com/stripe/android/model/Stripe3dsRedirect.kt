package com.stripe.android.model

import com.stripe.android.utils.Either

internal data class Stripe3dsRedirect internal constructor(val url: String) {
    internal companion object {
        private const val FIELD_STRIPE_JS = "stripe_js"

        internal fun create(sdkData: StripeIntent.SdkData): Stripe3dsRedirect {
            require(sdkData.is3ds1) {
                "Expected SdkData with type='three_d_secure_redirect'."
            }

            return when (sdkData.data) {
                is Either.Left -> {
                    val data = sdkData.data.left
                    Stripe3dsRedirect(
                        data[FIELD_STRIPE_JS] as String
                    )
                }
                is Either.Right -> {
                    val data = sdkData.data.right
                    require(data is PaymentIntent.NextActionData.SdkData.Use3D1) {
                        "Expected SdkData with type='three_d_secure_redirect'."
                    }
                    Stripe3dsRedirect(data.url)
                }
            }
        }
    }
}
