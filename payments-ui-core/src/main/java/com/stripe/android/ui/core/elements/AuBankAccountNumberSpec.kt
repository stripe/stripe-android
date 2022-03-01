package com.stripe.android.ui.core.elements

import kotlinx.parcelize.Parcelize

@Parcelize
internal object AuBankAccountNumberSpec : SectionFieldSpec(IdentifierSpec.Generic("account_number")) {
    fun transform(): SectionFieldElement =
        SimpleTextElement(
            this.identifier,
            TextFieldController(AuBankAccountNumberConfig())
        )
}
