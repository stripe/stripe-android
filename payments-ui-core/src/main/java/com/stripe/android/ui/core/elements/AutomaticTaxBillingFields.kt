package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.IdentifierSpec

/**
 * Billing address fields required in addition to the country, for a Checkout Session using
 * automatic tax with the billing address as the tax source. Most countries only need the
 * country. Source: https://docs.stripe.com/tax/customer-locations
 *
 * Billing only - shipping is out of scope, since it's always collected in full for delivery
 * regardless of tax, so there's no omittable mode there for tax to rescue.
 *
 * NOTE: values here are consumed by paymentsheet's hasSufficientBillingDetailsForAutomaticTax
 * (BillingDetailsValidation.kt), which maps each IdentifierSpec to a PaymentMethod.Address field
 * via an exhaustive `when` with an `else -> false` fallback. Only add IdentifierSpec values that
 * that `when` handles (Line1/City/State/PostalCode today), or update it in lockstep — otherwise a
 * new field here would silently over-filter every SPM in that country.
 */
private val additionalAutomaticTaxFieldsByCountry: Map<String, Set<IdentifierSpec>> = mapOf(
    "CA" to setOf(IdentifierSpec.PostalCode),
    "GB" to setOf(IdentifierSpec.PostalCode),
    "IN" to setOf(IdentifierSpec.PostalCode),
    "PR" to setOf(IdentifierSpec.Line1, IdentifierSpec.City, IdentifierSpec.PostalCode),
    "US" to setOf(IdentifierSpec.Line1, IdentifierSpec.City, IdentifierSpec.State, IdentifierSpec.PostalCode),
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun automaticTaxRequiredFields(countryCode: String): Set<IdentifierSpec> {
    return additionalAutomaticTaxFieldsByCountry[countryCode].orEmpty()
}
