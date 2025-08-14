package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult
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
    val elementsSessionId: String? = null,
    private val autoCardScanData: AutoCardScanData?,
) : SectionFieldErrorController {

    internal val cardDetailsElement = CardDetailsElement(
        IdentifierSpec.Generic("card_detail"),
        cardAccountRangeRepositoryFactory,
        initialValues,
        collectName,
        cbcEligibility,
        cardBrandFilter
    )

    internal val isCardScanEnabledAndAvailable = cardDetailsElement.isCardScanEnabled &&
        DefaultIsStripeCardScanAvailable().invoke()
    internal val shouldSeeAutomaticCardScanOpen = isCardScanEnabledAndAvailable &&
        autoCardScanData?.shouldOpenCardScanAutomatically == true

    fun setHasSeenAutoCardScanOpen() {
        this.autoCardScanData?.let {
            it.hasSeenAutoCardScanOpen = true
        }
    }

    override val error = cardDetailsElement.controller.error

    fun onCardScanResult(result: CardScanSheetResult) {
        cardDetailsElement.controller.numberElement.controller.onCardScanResult(result)
    }
}
