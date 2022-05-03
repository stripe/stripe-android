package com.stripe.android.ui.core.elements

import kotlinx.parcelize.Parcelize

@Parcelize
internal data class AuBecsDebitMandateTextSpec(
    override val api_path: IdentifierSpec = IdentifierSpec.Generic("au_becs_mandate")
) : FormItemSpec(), RequiredItemSpec {
    fun transform(merchantName: String): FormElement =
        AuBecsDebitMandateTextElement(
            this.api_path,
            merchantName,
        )
}
