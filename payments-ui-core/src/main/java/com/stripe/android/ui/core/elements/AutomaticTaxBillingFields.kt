package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.FormFieldId

/**
 * Billing address fields required in addition to country for automatic tax calculation.
 * Countries absent from this map require country only.
 *
 * Source: https://docs.stripe.com/tax/customer-locations
 */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val additionalAutomaticTaxFieldsByCountry: Map<String, Set<FormFieldId>> = mapOf(
    "CA" to setOf(FormFieldId.PostalCode),
    "GB" to setOf(FormFieldId.PostalCode),
    "IN" to setOf(FormFieldId.PostalCode),
    "PR" to setOf(FormFieldId.Line1, FormFieldId.City, FormFieldId.PostalCode),
    "US" to setOf(FormFieldId.Line1, FormFieldId.City, FormFieldId.State, FormFieldId.PostalCode),
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun automaticTaxRequiredFields(countryCode: String): Set<FormFieldId> {
    return additionalAutomaticTaxFieldsByCountry[countryCode].orEmpty()
}
