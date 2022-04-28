package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class IbanSpec(
    override val identifier: IdentifierSpec = IdentifierSpec.Generic("sepa_debit[iban]")
) : FormItemSpec(), RequiredItemSpec {
    fun transform(initialValues: Map<IdentifierSpec, String?>)= createSectionElement(
        IbanElement(
            this.identifier,
            SimpleTextFieldController(
                IbanConfig(),
                initialValue = initialValues[this.identifier]
            )
        )
    )
}
