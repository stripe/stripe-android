package com.stripe.android.common.validation

import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet

internal fun PaymentMethod.isSupportedWithBillingConfig(
    billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration
): Boolean = isSupportedWithConfig(
    billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
    countryCode = billingDetails?.address?.country,
)

internal fun ConsumerPaymentDetails.PaymentDetails.isSupportedWithBillingConfig(
    billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration
): Boolean = isSupportedWithConfig(
    billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
    countryCode = billingAddress?.countryCode?.value,
)

private fun isSupportedWithConfig(
    billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
    countryCode: String?,
): Boolean {
    val allowedCountries = billingDetailsCollectionConfiguration.allowedCountries

    if (allowedCountries.isEmpty()) {
        return true
    }

    return countryCode?.let { allowedCountries.contains(it) } ?: true
}
