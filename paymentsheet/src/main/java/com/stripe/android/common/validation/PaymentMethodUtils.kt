package com.stripe.android.common.validation

import com.stripe.android.core.model.CountryCode
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet

internal fun PaymentMethod.isSupportedWithBillingConfig(
    billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration
): Boolean {
    val allowedCountries = billingDetailsCollectionConfiguration.allowedBillingCountries

    if (allowedCountries.isEmpty()) {
        return true
    }

    val billingCountry = billingDetails?.address?.country?.uppercase()

    return billingCountry?.let {
        allowedCountries.contains(it)
    } ?: false
}

internal fun ConsumerPaymentDetails.PaymentDetails.isSupportedWithBillingConfig(
    billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration
): Boolean {
    val allowedCountries = billingDetailsCollectionConfiguration.allowedBillingCountries

    if (allowedCountries.isEmpty()) {
        return true
    }

    return when (this) {
        is ConsumerPaymentDetails.Card -> billingAddress?.countryCode?.let {
            allowedCountries.contains(it.value.uppercase())
        } ?: false
        is ConsumerPaymentDetails.BankAccount -> allowedCountries.contains(CountryCode.US.value)
    }
}
