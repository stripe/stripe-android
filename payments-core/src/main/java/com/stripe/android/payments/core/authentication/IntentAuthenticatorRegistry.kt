package com.stripe.android.payments.core.authentication

import com.stripe.android.model.StripeIntent

/**
 * Registry to map [StripeIntent] to the corresponding [IntentAuthenticator] to handle its next_action
 */
internal fun interface IntentAuthenticatorRegistry {

    /**
     * Returns the correct [IntentAuthenticator] to handle the [StripeIntent].
     */
    fun getAuthenticator(stripeIntent: StripeIntent): IntentAuthenticator
}
