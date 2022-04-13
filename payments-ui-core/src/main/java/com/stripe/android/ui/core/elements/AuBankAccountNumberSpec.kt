package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object AuBankAccountNumberSpec : SectionFieldSpec(IdentifierSpec.Generic("account_number")) {
    fun transform(): SectionFieldElement =
        SimpleTextElement(
            this.identifier,
            SimpleTextFieldController(AuBankAccountNumberConfig())
        )
}
