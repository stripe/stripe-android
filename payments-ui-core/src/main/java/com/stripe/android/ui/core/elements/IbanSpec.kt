package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
data class IbanSpec(
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("sepa_debit[iban]")
) : FormItemSpec() {
    fun transform(initialValues: Map<IdentifierSpec, String?>) = createSectionElement(
        IbanElement(
            this.apiPath,
            SimpleTextFieldController(
                IbanConfig(),
                initialValue = initialValues[this.apiPath]
            )
        )
    )
}
