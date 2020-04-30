package com.stripe.android.model

/**
 * Interface for params for confirming a [PaymentIntent] or [SetupIntent].
 *
 * See [ConfirmPaymentIntentParams] and [ConfirmSetupIntentParams]
 */
interface ConfirmStripeIntentParams : StripeParamsModel {

    val clientSecret: String

    fun shouldUseStripeSdk(): Boolean

    fun withShouldUseStripeSdk(shouldUseStripeSdk: Boolean): ConfirmStripeIntentParams

    companion object {
        internal const val PARAM_CLIENT_SECRET: String = "client_secret"
        internal const val PARAM_RETURN_URL: String = "return_url"
        internal const val PARAM_PAYMENT_METHOD_ID: String = "payment_method"
        internal const val PARAM_PAYMENT_METHOD_DATA: String = "payment_method_data"
        internal const val PARAM_USE_STRIPE_SDK: String = "use_stripe_sdk"
        internal const val PARAM_MANDATE_ID: String = "mandate"
        internal const val PARAM_MANDATE_DATA = "mandate_data"
    }
}
