package com.stripe.android.link.model

import com.stripe.android.link.ui.paymentmethod.SupportedPaymentMethod
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
        SupportedPaymentMethod.allTypes
    } else {
        linkFundingSources.filter { SupportedPaymentMethod.allTypes.contains(it) }
            .takeIf { it.isNotEmpty() }?.toSet()
            ?: setOf(ConsumerPaymentDetails.Card.type)
    }
