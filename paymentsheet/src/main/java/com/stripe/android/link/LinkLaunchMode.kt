package com.stripe.android.link

/**
 * The mode in which the Link flow is launched.
 */
internal enum class LinkLaunchMode {
    /**
     * Link is launched with the intent to solely authenticate.
     */
    Authentication,

    /**
     * Link is launched with the intent to obtain a payment method.
     */
    Payment,
}
