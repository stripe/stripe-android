package com.stripe.android.ui.core.elements

import android.content.Context
import com.stripe.android.ui.core.DefaultIsStripeCardScanAvailable

class CardDetailsSectionController(context: Context) : SectionFieldErrorController {

    internal val cardDetailsElement = CardDetailsElement(
        IdentifierSpec.Generic("card_detail"), context
    )

    internal val isStripeCardScanAvailable = DefaultIsStripeCardScanAvailable()

    override val error = cardDetailsElement.controller.error
}
