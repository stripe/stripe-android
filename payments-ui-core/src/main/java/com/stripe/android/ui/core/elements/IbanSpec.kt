package com.stripe.android.ui.core.elements

import kotlinx.parcelize.Parcelize

@Parcelize
internal object IbanSpec : SectionFieldSpec(IdentifierSpec.Generic("iban")) {
    fun transform(): SectionFieldElement =
        IbanElement(
            this.identifier,
            TextFieldController(IbanConfig())
        )
}
