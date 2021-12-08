package com.stripe.android.paymentsheet.elements

import kotlinx.parcelize.Parcelize

/**
 * This is the specification for a country field.
 * @property onlyShowCountryCodes: a list of country code that should be shown.  If empty all
 * countries will be shown.
 */
@Parcelize
internal data class CountrySpec(val onlyShowCountryCodes: Set<String> = emptySet()) :
    SectionFieldSpec(IdentifierSpec.Country)
