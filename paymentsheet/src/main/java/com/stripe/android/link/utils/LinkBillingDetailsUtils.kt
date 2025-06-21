package com.stripe.android.link.utils

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode

/**
 * Returns the effective billing details, which refers to billing details that have been
 * supplemented with billing information from the Link account. For instance, billing details
 * with a missing email address can be supplemented with the Link account's email address.
 *
 */
internal fun effectiveBillingDetails(
    configuration: LinkConfiguration,
    linkAccount: LinkAccount,
    originalBillingDetails: PaymentMethod.BillingDetails? = null
): PaymentMethod.BillingDetails {
    val billingConfig = configuration.billingDetailsCollectionConfiguration

    // Start with original billing details, or create from configuration defaults
    val baseBillingDetails = originalBillingDetails ?: PaymentMethod.BillingDetails(
        name = configuration.customerInfo.name,
        email = configuration.customerInfo.email,
        phone = configuration.customerInfo.phone,
        address = configuration.customerInfo.billingCountryCode?.let { countryCode ->
            com.stripe.android.model.Address(country = countryCode)
        }
    )

    // Supplement with Link account info when collection mode is [CollectionMode.Always]
    return baseBillingDetails.copy(
        email = if (billingConfig.email == CollectionMode.Always) {
            baseBillingDetails.email ?: linkAccount.email
        } else {
            baseBillingDetails.email
        },
        phone = if (billingConfig.phone == CollectionMode.Always) {
            baseBillingDetails.phone ?: linkAccount.unredactedPhoneNumber
        } else {
            baseBillingDetails.phone
        },
        name = if (billingConfig.name == CollectionMode.Always) {
            // We can't get the name from the consumer session, so we rely on what was originally provided
            baseBillingDetails.name
        } else {
            baseBillingDetails.name
        }
    )
}

/**
 * Extension function to check if a ConsumerPaymentDetails supports the given billing details collection configuration.
 * Returns true if the payment method has all required billing details based on the configuration.
 *
 */
internal fun ConsumerPaymentDetails.PaymentDetails.supports(
    billingDetailsConfig: PaymentSheet.BillingDetailsCollectionConfiguration,
    linkAccount: LinkAccount
): Boolean {
    // Only check for Card payment details, as other types don't have billing details requirements
    if (this !is ConsumerPaymentDetails.Card) {
        return true
    }

    // Check if name is required but missing
    if (billingDetailsConfig.name == CollectionMode.Always &&
        billingAddress?.name == null
    ) {
        return false
    }

    // Check if full address is required but missing or incomplete
    if (billingDetailsConfig.address == AddressCollectionMode.Full) {
        val address = billingAddress
        if (address == null || address.isIncomplete()) {
            return false
        }
    }

    // Check if phone is required but missing from Link account
    if (billingDetailsConfig.phone == CollectionMode.Always &&
        linkAccount.unredactedPhoneNumber == null
    ) {
        return false
    }

    // Email is always available from the Link account, so no need to check
    return true
}

/**
 * An address is considered incomplete if it's missing required fields for full address collection.
 */
private fun ConsumerPaymentDetails.BillingAddress.isIncomplete(): Boolean {
    return line1 == null || locality == null || postalCode == null || countryCode == null
}
