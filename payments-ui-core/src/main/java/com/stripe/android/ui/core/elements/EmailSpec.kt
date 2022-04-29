package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
object EmailSpec : SectionFieldSpec(IdentifierSpec.Email) {
    fun transform(initialValues: Map<IdentifierSpec, String?>): SectionFieldElement =
        EmailElement(
            this.identifier,
            SimpleTextFieldController(
                EmailConfig(),
                initialValue = initialValues[IdentifierSpec.Email]
            ),
        )
}
