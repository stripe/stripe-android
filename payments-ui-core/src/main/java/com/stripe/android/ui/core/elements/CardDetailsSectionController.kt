package com.stripe.android.ui.core.elements

import android.content.Context
import com.stripe.android.ui.core.DefaultIsStripeCardScanAvailable

class CardDetailsSectionController(
    context: Context,
    initialValues: Map<IdentifierSpec, String?>
) : SectionFieldErrorController {

    internal val cardDetailsElement = CardDetailsElement(
        IdentifierSpec.Generic("card_detail"),
        context,
        initialValues
    )

    internal val isStripeCardScanAvailable = DefaultIsStripeCardScanAvailable()

    override val error = cardDetailsElement.controller.error
}
