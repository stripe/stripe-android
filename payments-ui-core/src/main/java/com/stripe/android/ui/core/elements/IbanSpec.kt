package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
data class IbanSpec(
    override val apiPath: IdentifierSpec = DEFAULT_API_PATH
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

    companion object {
        val DEFAULT_API_PATH = IdentifierSpec.Generic("sepa_debit[iban]")
    }
}
