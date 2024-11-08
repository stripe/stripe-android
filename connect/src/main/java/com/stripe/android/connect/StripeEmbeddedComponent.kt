package com.stripe.android.connect

/**
 * Represents a given Stripe Embedded Component.
 * [componentName] matches the canonical name passed into the JS Stripe Connect Instance
 * `create` method: https://docs.stripe.com/connect/supported-embedded-components
 */
internal enum class StripeEmbeddedComponent(val componentName: String) {
    /**
     * Represents the Payouts component: https://docs.stripe.com/connect/supported-embedded-components/payouts
     */
    PAYOUTS("payouts"),
}
