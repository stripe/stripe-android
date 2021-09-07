package com.stripe.android.paymentsheet

import java.security.InvalidParameterException

internal fun PaymentSheet.Configuration.validate() {
    // These are not localized as they are not intended to be displayed to a user.
    when {
        merchantDisplayName.isBlank() -> {
            throw InvalidParameterException("Merchant display name cannot be blank.")
        }
        customer?.id?.isBlank() == true -> {
            throw InvalidParameterException("Customer id when specified cannot be blank.")
        }
        customer?.ephemeralKeySecret?.isBlank() == true -> {
            throw InvalidParameterException(
                "When the customer is specified the ephemeral" +
                    " key cannot be blank."
            )
        }
    }
}
