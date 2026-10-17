package com.stripe.android.paymentsheet.state

import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.elements.automaticTaxRequiredFields
import com.stripe.android.uicore.elements.FormFieldId

internal fun PaymentMethod.hasSufficientBillingDetailsForAutomaticTax(): Boolean {
    val address = billingDetails?.address ?: return false
    val countryCode = address.country?.takeUnless { it.isBlank() }?.uppercase() ?: return false

    return automaticTaxRequiredFields(countryCode).all { requiredField ->
        requiredField.addressValue(address).isNullOrBlank().not()
    }
}

private fun FormFieldId.addressValue(address: Address): String? {
    return when (this) {
        FormFieldId.Line1 -> address.line1
        FormFieldId.City -> address.city
        FormFieldId.State -> address.state
        FormFieldId.PostalCode -> address.postalCode
        else -> null
    }
}
