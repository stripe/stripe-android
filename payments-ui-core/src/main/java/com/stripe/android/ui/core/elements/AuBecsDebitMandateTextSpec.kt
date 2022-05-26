package com.stripe.android.ui.core.elements

import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class AuBecsDebitMandateTextSpec(
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("au_becs_mandate")
) : FormItemSpec(), RequiredItemSpec {
    fun transform(merchantName: String): FormElement =
        AuBecsDebitMandateTextElement(
            this.apiPath,
            merchantName,
        )
}
