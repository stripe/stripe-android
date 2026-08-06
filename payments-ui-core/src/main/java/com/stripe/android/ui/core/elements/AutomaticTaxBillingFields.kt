package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.IdentifierSpec

/**
 * Billing address fields required in addition to country for automatic tax calculation.
 * Countries absent from this map require country only.
 *
 * Source: https://docs.stripe.com/tax/customer-locations
 */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val additionalAutomaticTaxFieldsByCountry: Map<String, Set<IdentifierSpec>> = mapOf(
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
