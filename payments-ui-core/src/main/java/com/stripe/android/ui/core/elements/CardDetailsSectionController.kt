package com.stripe.android.ui.core.elements

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.DefaultIsStripeCardScanAvailable
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionFieldErrorController

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CardDetailsSectionController(
    context: Context,
    initialValues: Map<IdentifierSpec, String?>,
    collectName: Boolean = false,
    cbcEligibility: CardBrandChoiceEligibility = CardBrandChoiceEligibility.Ineligible,
) : SectionFieldErrorController {

    internal val cardDetailsElement = CardDetailsElement(
        IdentifierSpec.Generic("card_detail"),
        context,
        initialValues,
        collectName,
        cbcEligibility,
    )

    internal val isCardScanEnabled = cardDetailsElement.isCardScanEnabled
    internal val isStripeCardScanAvailable = DefaultIsStripeCardScanAvailable()

    override val error = cardDetailsElement.controller.error
}
