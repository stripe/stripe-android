package com.stripe.android.paymentsheet

import java.security.InvalidParameterException

internal fun PaymentSheet.Configuration.validate() {
    // These are not localized as they are not intended to be displayed to a user.
    when {
        merchantDisplayName.isBlank() -> {
            throw InvalidParameterException(
                "When a Configuration is passed to PaymentSheet," +
                    " the Merchant display name cannot be an empty string."
            )
        }
        customer?.id?.isBlank() == true -> {
            throw InvalidParameterException(
                "When a CustomerConfiguration is passed to PaymentSheet," +
                    " the id cannot be an empty string."
            )
        }
        customer?.ephemeralKeySecret?.isBlank() == true -> {
            throw InvalidParameterException(
                "When a CustomerConfiguration is passed to PaymentSheet, " +
                    "the ephemeralKeySecret cannot be an empty string."
            )
        }
    }
}
