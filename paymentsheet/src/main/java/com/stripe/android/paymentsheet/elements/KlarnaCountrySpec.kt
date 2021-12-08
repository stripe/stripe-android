package com.stripe.android.paymentsheet.elements

import kotlinx.parcelize.Parcelize

/**
 * This is the specification for a klarna country field
 */
@Parcelize
internal class KlarnaCountrySpec : SectionFieldSpec(IdentifierSpec.Country)
