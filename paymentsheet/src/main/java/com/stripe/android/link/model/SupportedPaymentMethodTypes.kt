package com.stripe.android.link.model

import com.stripe.android.model.StripeIntent

/**
 * Provides the supported payment method types for the given Link account.
 *
 * The supported payment methods are read from [StripeIntent.linkFundingSources]. Returns an empty
 * set when no funding sources are available; callers should treat this as an error condition
 * rather than falling back to card.
 */
internal fun StripeIntent.supportedPaymentMethodTypes(): Set<String> {
    return linkFundingSources.toSet()
}
