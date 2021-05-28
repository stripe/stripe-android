package com.stripe.android.payments.core.authentication

import com.stripe.android.model.StripeIntent

/**
 * Registry to map [StripeIntent] to the corresponding [IntentAuthenticator] to handle its next_action
 */
internal interface IntentAuthenticatorRegistry {

    /**
     * Returns the correct [IntentAuthenticator] to handle the [StripeIntent].
     */
    fun lookUp(stripeIntent: StripeIntent): IntentAuthenticator

    // TODO(ccen): Add registration API
}
