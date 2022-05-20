package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("au_becs_account_number")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class AuBankAccountNumberSpec(
    override val api_path: IdentifierSpec = IdentifierSpec.Generic("au_becs_debit[account_number]")
) : FormItemSpec(), RequiredItemSpec {
    fun transform(
        initialValues: Map<IdentifierSpec, String?>
    ) = createSectionElement(
        SimpleTextElement(
            this.api_path,
            SimpleTextFieldController(
                AuBankAccountNumberConfig(),
                initialValue = initialValues[this.api_path]
            )
        )
    )
}
