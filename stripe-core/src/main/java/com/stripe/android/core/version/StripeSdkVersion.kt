package com.stripe.android.core.version

/**
 * A class that represents the Stripe SDK version.
 *
 * See [https://github.com/stripe/stripe-android/releases](https://github.com/stripe/stripe-android/releases)
 * for changelog reference..
 *
 */
object StripeSdkVersion {
    const val VERSION_NAME = "23.7.0"
    const val VERSION: String = "AndroidBindings/$VERSION_NAME"

    // Revenue threshold for enterprise tier, based on Kavholm ($1000B) benchmark
    const val ENTERPRISE_REVENUE_THRESHOLD_USD = 1_000_000_000_000L
}
