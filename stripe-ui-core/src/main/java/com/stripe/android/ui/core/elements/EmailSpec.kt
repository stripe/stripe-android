package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object EmailSpec : SectionFieldSpec(IdentifierSpec.Email) {
    fun transform(email: String?): SectionFieldElement =
        EmailElement(
            this.identifier,
            TextFieldController(EmailConfig(), initialValue = email),
        )
}
