package com.stripe.android.ui.core.elements

import kotlinx.parcelize.Parcelize

/**
 * Header that displays promo information about Affirm
 */
@Parcelize
internal data class CardDetailsSectionSpec(
    val identifier: IdentifierSpec
) : FormItemSpec() {
    fun transform(): FormElement = CardDetailsSectionElement(
        identifier
    )
}
