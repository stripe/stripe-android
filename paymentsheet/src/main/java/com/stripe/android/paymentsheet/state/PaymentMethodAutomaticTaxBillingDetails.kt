package com.stripe.android.paymentsheet.state

import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.elements.automaticTaxRequiredFields
import com.stripe.android.uicore.elements.IdentifierSpec

internal fun PaymentMethod.hasSufficientBillingDetailsForAutomaticTax(): Boolean {
    val address = billingDetails?.address ?: return false
    val countryCode = address.country?.takeUnless { it.isBlank() }?.uppercase() ?: return false

    return automaticTaxRequiredFields(countryCode).all { requiredField ->
        requiredField.addressValue(address).isNullOrBlank().not()
    }
}

private fun IdentifierSpec.addressValue(address: Address): String? {
    return when (this) {
        IdentifierSpec.Line1 -> address.line1
        IdentifierSpec.City -> address.city
        IdentifierSpec.State -> address.state
        IdentifierSpec.PostalCode -> address.postalCode
        else -> null
    }
}
