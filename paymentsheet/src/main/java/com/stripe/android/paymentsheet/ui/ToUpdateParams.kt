package com.stripe.android.paymentsheet.ui

import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod

internal fun CardDetailsEntry.toUpdateParams(collectAddress: Boolean): CardUpdateParams {
    return CardUpdateParams(
        expiryMonth = expMonth,
        expiryYear = expYear,
        cardBrand = cardBrandChoice.brand,
        billingDetails = when (collectAddress) {
            true -> {
                val address = Address(
                    city = city?.value,
                    country = country?.value,
                    line1 = line1?.value,
                    line2 = line2?.value,
                    postalCode = postalCode?.value,
                    state = state?.value
                )
                PaymentMethod.BillingDetails(address)
            }
            false -> null
        }
    )
}