package com.stripe.android.ui.core.elements

import kotlinx.parcelize.Parcelize

@Parcelize
internal data class AuBecsDebitMandateTextSpec(
    override val identifier: IdentifierSpec,
    val fontSizeSp: Int = 10,
    val letterSpacingSp: Double = .7
) : FormItemSpec(), RequiredItemSpec {
    fun transform(merchantName: String): FormElement =

        AuBecsDebitMandateTextElement(
            this.identifier,
            merchantName,
            this.fontSizeSp,
            this.letterSpacingSp
        )
}
