package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
@Parcelize
data class IbanSpec(
    override val api_path: IdentifierSpec = IdentifierSpec.Generic("sepa_debit[iban]")
) : FormItemSpec(), RequiredItemSpec {
    fun transform(initialValues: Map<IdentifierSpec, String?>) = createSectionElement(
        IbanElement(
            this.api_path,
            SimpleTextFieldController(
                IbanConfig(),
                initialValue = initialValues[this.api_path]
            )
        )
    )
}
