package com.stripe.android.crypto.onramp.exception

import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * A wrapper SDK name and version pair included in developer diagnostics.
 *
 * Do not use this type for the Stripe Android SDK version. Stripe Android is always included
 * automatically.
 */
@ExperimentalCryptoOnramp
data class SDKVersion(
    /**
     * The SDK name.
     */
    val name: String,
    /**
     * The SDK version.
     */
    val version: String,
) {
    /**
     * A developer-facing SDK version description.
     */
    val debugDescription: String
        get() = "$name@$version"

    internal companion object {
        val stripeAndroid = SDKVersion(
            name = "stripe-android",
            version = StripeSdkVersion.VERSION,
        )
    }
}
