package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object AuBankAccountNumberSpec :
    SectionFieldSpec(IdentifierSpec.Generic("au_becs_debit[account_number]")) {
    fun transform(
        initialValues: Map<IdentifierSpec, String?>
    ): SectionFieldElement =
        SimpleTextElement(
            this.identifier,
            SimpleTextFieldController(
                AuBankAccountNumberConfig(),
                initialValue = initialValues[this.identifier]
            )
        )
}
