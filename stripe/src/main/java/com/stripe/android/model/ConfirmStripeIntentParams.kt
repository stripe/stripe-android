package com.stripe.android.model

interface ConfirmStripeIntentParams : StripeParamsModel {

    val clientSecret: String

    fun shouldUseStripeSdk(): Boolean

    fun withShouldUseStripeSdk(shouldUseStripeSdk: Boolean): ConfirmStripeIntentParams

    companion object {
        const val API_PARAM_CLIENT_SECRET = "client_secret"
        const val API_PARAM_RETURN_URL = "return_url"
        const val API_PARAM_PAYMENT_METHOD_ID = "payment_method"
        const val API_PARAM_PAYMENT_METHOD_DATA = "payment_method_data"
        const val API_PARAM_USE_STRIPE_SDK = "use_stripe_sdk"
    }
}
