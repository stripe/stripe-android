package com.stripe.android.common.validation

import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.elements.automaticTaxRequiredFields
import com.stripe.android.uicore.elements.IdentifierSpec

/**
 * Does this saved payment method's billing address have what Stripe Tax needs to compute
 * automatic tax for its country? See automaticTaxRequiredFields for the per-country field set.
 */
internal fun PaymentMethod.hasSufficientBillingDetailsForAutomaticTax(): Boolean {
    val address = billingDetails?.address ?: return false
    // Saved-PM billing details come from the API and are not guaranteed uppercase, unlike the
    // CountryConfig-sourced value the CardBillingAddressElement form passes. Uppercase before the
    // map lookup; do not "simplify" this away.
    val country = address.country?.uppercase()
    if (country.isNullOrBlank()) return false

    return automaticTaxRequiredFields(country).all { field ->
        when (field) {
            IdentifierSpec.Line1 -> !address.line1.isNullOrBlank()
            IdentifierSpec.City -> !address.city.isNullOrBlank()
            IdentifierSpec.State -> !address.state.isNullOrBlank()
            IdentifierSpec.PostalCode -> !address.postalCode.isNullOrBlank()
            // additionalAutomaticTaxFieldsByCountry only emits the four specs above today. If that
            // map ever adds another (e.g. Line2), add its case here — else it silently over-filters.
            else -> false
        }
    }
}
