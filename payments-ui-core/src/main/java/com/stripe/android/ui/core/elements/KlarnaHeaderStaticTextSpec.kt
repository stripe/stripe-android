package com.stripe.android.ui.core.elements

import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This is for the Klarna header
 */
@Serializable
internal data class KlarnaHeaderStaticTextSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("klarna_header_text")
) : FormItemSpec() {
    fun transform(): FormElement =
        // It could be argued that the static text should have a controller, but
        // since it doesn't provide a form field we leave it out for now
        StaticTextElement(
            this.apiPath,
            stringResId = KlarnaHelper.getKlarnaHeader()
        )
}
