package com.stripe.android.ui.core.elements

import android.content.Context
import kotlinx.parcelize.Parcelize

/**
 * Section containing card details form
 */
@Parcelize
internal data class CardDetailsSectionSpec(
    val identifier: IdentifierSpec
) : FormItemSpec() {
    fun transform(context: Context): FormElement = CardDetailsSectionElement(
        context, identifier
    )
}
