package com.stripe.android.ui.core.elements

import com.stripe.android.ui.core.Amount
import kotlinx.parcelize.Parcelize

/**
 * Header that displays promo information about Affirm
 */
@Parcelize
internal data class AffirmTextSpec(
    val identifier: IdentifierSpec
) : FormItemSpec() {
    fun transform(): FormElement =
        AffirmHeaderElement(this.identifier)
}
