package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.ui.core.DefaultIsStripeCardScanAvailable
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionFieldErrorController

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CardDetailsSectionController(
    cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    initialValues: Map<IdentifierSpec, String?>,
    collectName: Boolean = false,
    cbcEligibility: CardBrandChoiceEligibility = CardBrandChoiceEligibility.Ineligible,
    cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
) : SectionFieldErrorController {

    internal val cardDetailsElement = CardDetailsElement(
        IdentifierSpec.Generic("card_detail"),
        cardAccountRangeRepositoryFactory,
        initialValues,
        collectName,
        cbcEligibility,
        cardBrandFilter
    )

    internal val isCardScanEnabled = cardDetailsElement.isCardScanEnabled
    internal val isStripeCardScanAvailable = DefaultIsStripeCardScanAvailable()

    override val error = cardDetailsElement.controller.error
}
