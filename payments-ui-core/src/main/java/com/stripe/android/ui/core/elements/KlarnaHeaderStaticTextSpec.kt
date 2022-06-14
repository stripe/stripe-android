package com.stripe.android.ui.core.elements

import kotlinx.serialization.Serializable

/**
 * This is for the Klarna header
 */
@Serializable
internal data class KlarnaHeaderStaticTextSpec(
    override val apiPath: IdentifierSpec = DEFAULT_API_PATH
) : FormItemSpec() {
    fun transform(): FormElement =
        // It could be argued that the static text should have a controller, but
        // since it doesn't provide a form field we leave it out for now
        StaticTextElement(
            this.apiPath,
            stringResId = KlarnaHelper.getKlarnaHeader(),
        )

    companion object {
        val DEFAULT_API_PATH = IdentifierSpec.Generic("klarna_header_text")
    }
}
