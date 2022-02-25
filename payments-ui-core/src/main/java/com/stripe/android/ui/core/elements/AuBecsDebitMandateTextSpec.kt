package com.stripe.android.ui.core.elements

import androidx.annotation.ColorRes
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class AuBecsDebitMandateTextSpec(
    override val identifier: IdentifierSpec,
    @ColorRes val color: Int? = null,
    val fontSizeSp: Int = 10,
    val letterSpacingSp: Double = .7
) : FormItemSpec(), RequiredItemSpec {
    fun transform(merchantName: String): FormElement =

        AuBecsDebitMandateTextElement(
            this.identifier,
            this.color,
            merchantName,
            this.fontSizeSp,
            this.letterSpacingSp
        )
}
