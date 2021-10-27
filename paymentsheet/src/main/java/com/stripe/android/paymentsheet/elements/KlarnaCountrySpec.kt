package com.stripe.android.paymentsheet.elements

/**
 * This is the specification for a klarna country field
 * @property onlyShowCountryCodes: a list of country code that should be shown.  If empty all
 * countries will be shown.
 */
internal data class KlarnaCountrySpec(val onlyShowCountryCodes: Set<String> = emptySet()) :
    SectionFieldSpec(IdentifierSpec.Country)
