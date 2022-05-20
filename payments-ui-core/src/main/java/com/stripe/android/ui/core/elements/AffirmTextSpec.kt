package com.stripe.android.ui.core.elements

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Header that displays promo information about Affirm
 */
@Serializable
@SerialName("affirm_header")
internal data class AffirmTextSpec(
    override val api_path: IdentifierSpec
) : FormItemSpec() {
    fun transform(): FormElement =
        AffirmHeaderElement(this.api_path)
}
