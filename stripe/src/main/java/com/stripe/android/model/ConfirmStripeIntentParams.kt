package com.stripe.android.model

interface ConfirmStripeIntentParams : StripeParamsModel {

    val clientSecret: String

    fun shouldUseStripeSdk(): Boolean

    fun withShouldUseStripeSdk(shouldUseStripeSdk: Boolean): ConfirmStripeIntentParams

    companion object {
        internal const val API_PARAM_CLIENT_SECRET: String = "client_secret"
        internal const val API_PARAM_RETURN_URL: String = "return_url"
        internal const val API_PARAM_PAYMENT_METHOD_ID: String = "payment_method"
        internal const val API_PARAM_PAYMENT_METHOD_DATA: String = "payment_method_data"
        internal const val API_PARAM_USE_STRIPE_SDK: String = "use_stripe_sdk"
    }
}
