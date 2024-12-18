package com.stripe.android.link.model

import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.StripeIntent

/**
 * Provides the supported payment method types for the given Link account.
 *
 * In test mode, accounts with email in the format {any_prefix}+multiple_funding_sources@{any_domain}
 * enable all payment method types supported by the SDK.
 *
 * The supported payment methods are read from [StripeIntent.linkFundingSources], and fallback to
 * card only if the list is empty or none of them is valid.
 */
internal fun StripeIntent.supportedPaymentMethodTypes(linkAccount: LinkAccount) =
    if (!isLiveMode && linkAccount.email.contains("+multiple_funding_sources@")) {
        supportedPaymentMethodTypes
    } else {
        linkFundingSources.filter { supportedPaymentMethodTypes.contains(it) }
            .takeIf { it.isNotEmpty() }?.toSet()
            ?: setOf(ConsumerPaymentDetails.Card.TYPE)
    }

private val supportedPaymentMethodTypes = setOf(
    ConsumerPaymentDetails.Card.TYPE,
    ConsumerPaymentDetails.BankAccount.TYPE,
    ConsumerPaymentDetails.Passthrough.TYPE
)
