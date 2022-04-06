package com.stripe.android.ui.core.elements

import android.content.Context

class CardDetailsSectionController(context: Context) : SectionFieldErrorController {

    internal val cardDetailsElement = CardDetailsElement(
        IdentifierSpec.Generic("card_detail"), context
    )

    override val error = cardDetailsElement.controller.error
}
