package com.stripe.android.ui.core.elements

import kotlinx.parcelize.Parcelize

@Parcelize
internal data class AuBecsDebitMandateTextSpec(
    override val api_path: IdentifierSpec
) : FormItemSpec(), RequiredItemSpec {
    fun transform(merchantName: String): FormElement =
        AuBecsDebitMandateTextElement(
            this.api_path,
            merchantName,
        )
}
