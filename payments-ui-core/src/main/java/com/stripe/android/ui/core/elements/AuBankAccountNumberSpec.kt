package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
class AuBankAccountNumberSpec(
    override val apiPath: IdentifierSpec = DEFAULT_API_PATH
) : FormItemSpec() {
    fun transform(
        initialValues: Map<IdentifierSpec, String?>
    ) = createSectionElement(
        SimpleTextElement(
            this.apiPath,
            SimpleTextFieldController(
                AuBankAccountNumberConfig(),
                initialValue = initialValues[this.apiPath]
            )
        )
    )

    companion object {
        val DEFAULT_API_PATH = IdentifierSpec.Generic(
            "au_becs_debit[account_number]"
        )
    }
}
