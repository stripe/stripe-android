package com.stripe.android.link.model

import com.stripe.android.model.StripeIntent

/**
 * Provides the supported payment method types for the given Link account.
 *
 * Computes the intersection of [StripeIntent.linkFundingSources] (merchant-configured) and
 * [consumerSupportedPaymentDetailsTypes] (consumer-level, from the /confirm_verification response).
 * Comparison is case-insensitive since the two sources use different casing conventions.
 *
 * When [consumerSupportedPaymentDetailsTypes] is empty (not yet available), returns all
 * [StripeIntent.linkFundingSources] to avoid restricting before the consumer session is known.
 * Returns an empty set when no funding sources are available; callers should treat this as an error
 * condition rather than falling back to card.
 */
internal fun StripeIntent.supportedPaymentMethodTypes(
    consumerSupportedPaymentDetailsTypes: List<String>,
): Set<String> {
    val fundingSources = linkFundingSources.toSet()
    if (consumerSupportedPaymentDetailsTypes.isEmpty()) return fundingSources
    val consumerTypesLower = consumerSupportedPaymentDetailsTypes.map { it.lowercase() }.toSet()
    return fundingSources.filter { it.lowercase() in consumerTypesLower }.toSet()
}
