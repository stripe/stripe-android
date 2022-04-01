package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

/**
 * This is the specification for a klarna country field
 */
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class KlarnaCountrySpec : SectionFieldSpec(IdentifierSpec.Country) {
    fun transform(currencyCode: String?, country: String?): SectionFieldElement =
        CountryElement(
            this.identifier,
            DropdownFieldController(
                CountryConfig(KlarnaHelper.getAllowedCountriesForCurrency(currencyCode)), country
            )
        )
}
