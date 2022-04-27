package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class AuBankAccountNumberSpec(
    override val identifier: IdentifierSpec = IdentifierSpec.Generic("au_becs_debit[account_number]")
) : FormItemSpec(), RequiredItemSpec {
    fun transform(
        initialValues: Map<IdentifierSpec, String?>
    ): SectionElement {
        val sectionFieldElement =
            SimpleTextElement(
                this.identifier,
                SimpleTextFieldController(
                    AuBankAccountNumberConfig(),
                    initialValue = initialValues[this.identifier]
                )
            )
        return SectionElement(
            IdentifierSpec.Generic(identifier.value + "_section"),
            sectionFieldElement,
            SectionController(
                null,
                listOf(sectionFieldElement.sectionFieldErrorController())
            )
        )
    }
}
