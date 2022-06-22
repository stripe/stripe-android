package com.stripe.android.ui.core.elements

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Header that displays promo information about Affirm
 */
@Serializable
internal data class AffirmTextSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = DEFAULT_API_PATH
) : FormItemSpec() {
    fun transform(): FormElement =
        AffirmHeaderElement(this.apiPath)

    companion object {
        val DEFAULT_API_PATH = IdentifierSpec.Generic("affirm_header")
    }
}
