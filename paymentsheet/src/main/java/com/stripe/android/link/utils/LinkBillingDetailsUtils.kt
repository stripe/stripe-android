package com.stripe.android.link.utils

import com.stripe.android.core.model.CountryCode
import com.stripe.android.elements.BillingDetails
import com.stripe.android.elements.BillingDetailsCollectionConfiguration
import com.stripe.android.elements.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetails.PaymentDetails
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.toPaymentSheetBillingDetails

/**
 * Returns the effective billing details, which refers to billing details that have been
 * supplemented with billing information from the Link account. For instance, billing details
 * with a missing email address can be supplemented with the Link account's email address.
 */
internal fun effectiveBillingDetails(
    configuration: LinkConfiguration,
    linkAccount: LinkAccount,
): BillingDetails {
    val billingConfig = configuration.billingDetailsCollectionConfiguration
    val defaultBillingDetails = configuration.defaultBillingDetails
    return BillingDetails(
        email = defaultBillingDetails?.email
            ?: linkAccount.email.takeIf { billingConfig.collectsEmail },
        phone = defaultBillingDetails?.phone
            ?: linkAccount.unredactedPhoneNumber.takeIf { billingConfig.collectsPhone },
        // Name and address cannot be supplemented from Link account data
        name = defaultBillingDetails?.name,
        address = defaultBillingDetails?.address
    )
}

/**
 * Returns true if the payment method has all required billing details based on the configuration.
 */
internal fun PaymentDetails.supports(
    billingDetailsConfig: BillingDetailsCollectionConfiguration,
    linkAccount: LinkAccount
): Boolean = when (this) {
    is ConsumerPaymentDetails.BankAccount,
    is ConsumerPaymentDetails.Card -> when {
        // Check if full address is required but missing or incomplete
        billingDetailsConfig.address == AddressCollectionMode.Full && billingAddress.isIncomplete() -> false
        // Check if phone is required but missing from Link account
        billingDetailsConfig.collectsPhone && linkAccount.unredactedPhoneNumber == null -> false
        // Check if name is required but missing
        billingDetailsConfig.collectsName && billingAddress?.name == null -> false
        // All billing configuration details are supported.
        else -> true
    }
    // Other payment details don't have billing details requirements
    else -> true
}

/**
 * An address is considered incomplete if it's missing required fields for full address collection.
 */
private fun ConsumerPaymentDetails.BillingAddress?.isIncomplete(): Boolean {
    return this == null || line1 == null || locality == null || postalCode == null || countryCode == null
}

/**
 * Enhances a ConsumerPaymentDetails.Card's billing details with [effectiveBillingDetails].
 * This is used for prefilling billing details in update flows.
 */
internal fun PaymentDetails.withEffectiveBillingDetails(
    configuration: LinkConfiguration,
    linkAccount: LinkAccount?
): PaymentDetails {
    if (linkAccount == null) return this
    val effectiveBillingDetails = effectiveBillingDetails(configuration, linkAccount)
    val effectiveAddress = effectiveBillingDetails.toConsumerBillingAddress()
    val addressesAreCompatible = billingAddress?.let { current ->
        effectiveAddress?.countryCode == current.countryCode &&
            effectiveAddress?.postalCode == current.postalCode
    } == true
    // Prefer effective address when current is missing or addresses are compatible,
    // otherwise keep the current address to avoid data conflicts
    val effectiveBillingAddress = when {
        billingAddress == null || addressesAreCompatible -> effectiveAddress
        else -> billingAddress
    }

    val effectiveEmailAddress = effectiveBillingDetails.email ?: billingEmailAddress

    return when (this) {
        is ConsumerPaymentDetails.Card -> copy(
            billingAddress = effectiveBillingAddress,
            billingEmailAddress = effectiveEmailAddress
        )
        is ConsumerPaymentDetails.BankAccount -> copy(
            billingAddress = effectiveBillingAddress,
            billingEmailAddress = effectiveEmailAddress
        )
        is ConsumerPaymentDetails.Passthrough -> this
    }
}

private fun BillingDetails.toConsumerBillingAddress(): ConsumerPaymentDetails.BillingAddress? {
    val billingAddress = address ?: return null
    return ConsumerPaymentDetails.BillingAddress(
        name = name,
        line1 = billingAddress.line1,
        line2 = billingAddress.line2,
        locality = billingAddress.city,
        administrativeArea = billingAddress.state,
        postalCode = billingAddress.postalCode,
        countryCode = billingAddress.country?.let { CountryCode.create(it) }
    )
}

internal fun PaymentMethod.BillingDetails.toConsumerBillingAddress(): ConsumerPaymentDetails.BillingAddress? {
    return this.toPaymentSheetBillingDetails().toConsumerBillingAddress()
}
