package com.stripe.android

import android.os.Parcelable
import com.stripe.android.core.ApiKeyValidator
import kotlinx.parcelize.Parcelize

/**
 * Holds per-instance Stripe API credentials. Pass this to payment UI components
 * to override the global [PaymentConfiguration] singleton for that instance.
 */
@Parcelize
data class StripeClient(
    val publishableKey: String,
    val stripeAccountId: String? = null,
) : Parcelable {
    init {
        ApiKeyValidator.get().requireValid(publishableKey)
    }
}
