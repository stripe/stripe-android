package com.stripe.android.ui.core.elements

import androidx.annotation.StringRes
import kotlinx.serialization.Serializable

/**
 * This is for elements that do not receive user input
 */
@Serializable
internal data class StaticTextSpec(
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("static_text"),
    @StringRes val stringResId: Int
) : FormItemSpec() {
    fun transform(): FormElement =
        // It could be argued that the static text should have a controller, but
        // since it doesn't provide a form field we leave it out for now
        StaticTextElement(
            this.apiPath,
            this.stringResId
        )
}
