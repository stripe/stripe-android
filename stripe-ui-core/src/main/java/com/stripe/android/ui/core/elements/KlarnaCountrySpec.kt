package com.stripe.android.ui.core.elements

/**
 * This is the specification for a klarna country field
 */
internal class KlarnaCountrySpec : SectionFieldSpec(IdentifierSpec.Country) {
    fun transform(currencyCode: String?, country: String?): SectionFieldElement =
        CountryElement(
            this.identifier,
            DropdownFieldController(
                CountryConfig(KlarnaHelper.getAllowedCountriesForCurrency(currencyCode)), country
            )
        )
}
