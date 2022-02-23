package com.stripe.android.ui.core.elements

import kotlinx.parcelize.Parcelize

@Parcelize
internal object BsbSpec : SectionFieldSpec(IdentifierSpec.Generic("bsb_number")) {
    fun transform(): SectionFieldElement =
        BsbElement(
            this.identifier,
            TextFieldController(BsbConfig())
        )
}
