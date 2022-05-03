package com.stripe.android.ui.core.elements

import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

/**
 * This is for elements that do not receive user input
 */
@Parcelize
internal data class StaticTextSpec(
    override val api_path: IdentifierSpec,
    @StringRes val stringResId: Int
) : FormItemSpec(), RequiredItemSpec {
    fun transform(): FormElement =
        // It could be argued that the static text should have a controller, but
        // since it doesn't provide a form field we leave it out for now
        StaticTextElement(
            this.api_path,
            this.stringResId
        )
}
