package com.stripe.android.ui.core.elements

import com.stripe.android.ui.core.PaymentsThemeConfig
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class AuBecsDebitMandateTextSpec(
    override val identifier: IdentifierSpec,
    val fontSize: Float = PaymentsThemeConfig.Typography.body2.fontSize.value,
    val letterSpacing: Float = PaymentsThemeConfig.Typography.body2.letterSpacing.value,
) : FormItemSpec(), RequiredItemSpec {
    fun transform(merchantName: String): FormElement =

        AuBecsDebitMandateTextElement(
            this.identifier,
            merchantName,
            this.fontSize,
            this.letterSpacing
        )
}
