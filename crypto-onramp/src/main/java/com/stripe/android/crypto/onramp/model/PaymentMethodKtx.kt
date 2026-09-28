package com.stripe.android.crypto.onramp.model

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet

internal fun PaymentMethod.googlePayKycInfo(): KycInfo? {
    val address = billingDetails?.address

    // Google Pay on Android exposes billing name as a single free-form string.
    // We split on whitespace as a best-effort heuristic to populate first/last name,
    // with the assumption that the last name will be the last word, while the first name
    // is everything preceding it.
    val fullName = billingDetails?.name.orEmpty().trim()
    val parts = fullName.split("\\s+".toRegex())
    val firstName = when {
        parts.size == 1 -> parts.firstOrNull().orEmpty()
        else -> parts.dropLast(1).joinToString(" ")
    }
    val lastName = when {
        parts.size <= 1 -> ""
        else -> parts.lastOrNull().orEmpty()
    }

    val hasName = firstName.isNotEmpty() || lastName.isNotEmpty()
    val hasAddress = listOfNotNull(
        address?.city,
        address?.country,
        address?.line1,
        address?.line2,
        address?.postalCode,
        address?.state,
    ).any { it.isNotBlank() }

    if (!hasName && !hasAddress) return null

    return KycInfo(
        firstName = firstName.takeIf { it.isNotEmpty() },
        lastName = lastName.takeIf { it.isNotEmpty() },
        idNumber = null,
        dateOfBirth = null,
        address = if (hasAddress) {
            PaymentSheet.Address(
                city = address?.city,
                country = address?.country,
                line1 = address?.line1,
                line2 = address?.line2,
                postalCode = address?.postalCode,
                state = address?.state
            )
        } else {
            null
        }
    )
}
