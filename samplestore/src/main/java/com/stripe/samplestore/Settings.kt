package com.stripe.samplestore

import com.stripe.android.model.PaymentMethod

/**
 * See [Configuring the samplestore app](https://github.com/stripe/stripe-android#configuring-the-samplestore-app)
 * for instructions on how to configure the app before running it.
 */
internal object Settings {
    /**
     * Set to the base URL of your test backend. If you are using
     * [example-ios-backend](https://github.com/stripe/example-ios-backend),
     * the URL will be something like `https://hidden-beach-12345.herokuapp.com/`.
     */
    const val BASE_URL = "put your base url here"

    /**
     * Set to publishable key from https://dashboard.stripe.com/test/apikeys
     */
    const val PUBLISHABLE_KEY = "pk_test_your_key_goes_here"

    /**
     * Optionally, set to a Connect Account id to use for API requests to test Connect
     *
     * See https://dashboard.stripe.com/test/connect/accounts/overview
     */
    val STRIPE_ACCOUNT_ID: String? = null

    /**
     * Three-letter ISO [currency code](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-currency),
     * in lowercase. Must be a [supported currency](https://stripe.com/docs/currencies).
     */
    const val CURRENCY = "usd"

    /**
     * The list of [payment method types](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-payment_method_types)
     * (e.g. card) that this PaymentIntent is allowed to use.
     */
    val ALLOWED_PAYMENT_METHOD_TYPES = listOf(
        PaymentMethod.Type.Card
    )
}
