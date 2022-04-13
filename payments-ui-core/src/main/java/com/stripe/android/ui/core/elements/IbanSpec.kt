package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object IbanSpec : SectionFieldSpec(IdentifierSpec.Generic("iban")) {
    fun transform(): SectionFieldElement =
        IbanElement(
            this.identifier,
            SimpleTextFieldController(IbanConfig())
        )
}
