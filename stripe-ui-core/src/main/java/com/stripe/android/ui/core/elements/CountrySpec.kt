package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo

/**
 * This is the specification for a country field.
 * @property onlyShowCountryCodes: a list of country code that should be shown.  If empty all
 * countries will be shown.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class CountrySpec(val onlyShowCountryCodes: Set<String> = emptySet()) :
    SectionFieldSpec(IdentifierSpec.Country) {
    fun transform(country: String?): SectionFieldElement =
        CountryElement(
            this.identifier,
            DropdownFieldController(CountryConfig(this.onlyShowCountryCodes), country)
        )
}
