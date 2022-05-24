package com.stripe.android.ui.core.elements

import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * This is for elements that do not receive user input
 */
@Serializable
@Parcelize
internal data class KlarnaHeaderStaticTextSpec(
    override val api_path: IdentifierSpec = IdentifierSpec.Generic("klarna_header_text")
) : FormItemSpec(), RequiredItemSpec {
    fun transform(): FormElement =
        // It could be argued that the static text should have a controller, but
        // since it doesn't provide a form field we leave it out for now
        StaticTextElement(
            this.api_path,
            stringResId = KlarnaHelper.getKlarnaHeader(),
        )
}
