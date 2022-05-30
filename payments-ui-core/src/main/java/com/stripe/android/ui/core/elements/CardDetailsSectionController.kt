package com.stripe.android.ui.core.elements

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.DefaultIsStripeCardScanAvailable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CardDetailsSectionController(
    context: Context,
    initialValues: Map<IdentifierSpec, String?>,
    viewOnlyFields: Set<IdentifierSpec>
) : SectionFieldErrorController {

    internal val cardDetailsElement = CardDetailsElement(
        IdentifierSpec.Generic("card_detail"),
        context,
        initialValues,
        viewOnlyFields
    )

    internal val isStripeCardScanAvailable = DefaultIsStripeCardScanAvailable()

    override val error = cardDetailsElement.controller.error
}
