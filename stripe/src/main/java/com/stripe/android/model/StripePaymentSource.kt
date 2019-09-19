package com.stripe.android.model

/**
 * Represents an object that has an ID field that can be used to create payments with Stripe.
 */
interface StripePaymentSource {
    val id: String?
}
