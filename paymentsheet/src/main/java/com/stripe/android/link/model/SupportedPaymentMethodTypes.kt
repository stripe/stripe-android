package com.stripe.android.link.model

import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.StripeIntent

/**
 * Provides the supported payment method types for the given Link account.
 *
 * The supported payment methods are read from [StripeIntent.linkFundingSources], and fallback to
 * card only if the list is empty or none of them is valid.
 */
internal fun StripeIntent.supportedPaymentMethodTypes(): Set<String> {
    return linkFundingSources.toSet().takeIf { it.isNotEmpty() } ?: setOf(ConsumerPaymentDetails.Card.TYPE)
}