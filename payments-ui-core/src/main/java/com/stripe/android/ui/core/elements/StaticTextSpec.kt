package com.stripe.android.ui.core.elements

import androidx.annotation.StringRes
import com.stripe.android.ui.core.PaymentsThemeConfig
import kotlinx.parcelize.Parcelize

/**
 * This is for elements that do not receive user input
 */
@Parcelize
internal data class StaticTextSpec(
    override val identifier: IdentifierSpec,
    @StringRes val stringResId: Int,
    val fontSize: Float = PaymentsThemeConfig.Typography.body2.fontSize.value,
    val letterSpacing: Float = PaymentsThemeConfig.Typography.body2.letterSpacing.value
) : FormItemSpec(), RequiredItemSpec {
    fun transform(merchantName: String): FormElement =
        // It could be argued that the static text should have a controller, but
        // since it doesn't provide a form field we leave it out for now
        StaticTextElement(
            this.identifier,
            this.stringResId,
            merchantName,
            this.fontSize,
            this.letterSpacing
        )
}
