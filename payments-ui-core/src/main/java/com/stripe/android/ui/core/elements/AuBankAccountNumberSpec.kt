package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
@Parcelize
class AuBankAccountNumberSpec(
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("au_becs_debit[account_number]")
) : FormItemSpec(), RequiredItemSpec {
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
}
