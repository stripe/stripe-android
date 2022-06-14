package com.stripe.android.ui.core.elements

import kotlinx.serialization.Serializable

@Serializable
internal data class AuBecsDebitMandateTextSpec(
    override val apiPath: IdentifierSpec = DEFAULT_API_PATH
) : FormItemSpec() {
    fun transform(merchantName: String): FormElement =
        AuBecsDebitMandateTextElement(
            this.apiPath,
            merchantName,
        )

    companion object {
        val DEFAULT_API_PATH = IdentifierSpec.Generic("au_becs_mandate")
    }
}
