package com.stripe.android.ui.core.elements

import kotlinx.serialization.Serializable

@Serializable
internal data class AuBecsDebitMandateTextSpec(
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("au_becs_mandate")
) : FormItemSpec() {
    fun transform(merchantName: String): FormElement =
        AuBecsDebitMandateTextElement(
            this.apiPath,
            merchantName,
        )
}
