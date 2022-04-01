package com.stripe.android.ui.core.elements

import kotlinx.parcelize.Parcelize

/**
 * Section containing card details form
 */
@Parcelize
internal data class CardDetailsSectionSpec(
    val identifier: IdentifierSpec
) : FormItemSpec() {
    fun transform(): FormElement = CardDetailsSectionElement(
        identifier
    )
}
