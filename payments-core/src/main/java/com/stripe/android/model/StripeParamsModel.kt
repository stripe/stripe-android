package com.stripe.android.model

/**
 * Model for a Stripe API object creation parameters
 */
interface StripeParamsModel {
    fun toParamMap(): Map<String, Any>
}
