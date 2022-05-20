package com.stripe.android.ui.core.elements

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("au_becs_mandate")
internal data class AuBecsDebitMandateTextSpec(
    override val api_path: IdentifierSpec = IdentifierSpec.Generic("au_becs_mandate")
) : FormItemSpec(), RequiredItemSpec {
    fun transform(merchantName: String): FormElement =
        AuBecsDebitMandateTextElement(
            this.api_path,
            merchantName,
        )
}
