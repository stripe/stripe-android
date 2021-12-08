package com.stripe.android.paymentsheet.elements

import kotlinx.parcelize.Parcelize

@Parcelize
internal object IbanSpec : SectionFieldSpec(IdentifierSpec.Generic("iban"))
