package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class EmailSpec(
    override val identifier: IdentifierSpec = IdentifierSpec.Email
) : FormItemSpec(), RequiredItemSpec {
    fun transform(initialValues: Map<IdentifierSpec, String?>): SectionElement {
        val sectionFieldElement = EmailElement(
            this.identifier,
            initialValue = initialValues[IdentifierSpec.Email]
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
