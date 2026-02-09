package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.ui.core.cardscan.CardScanResult
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionFieldValidationController

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CardDetailsSectionController(
    cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    initialValues: Map<IdentifierSpec, String?>,
    collectName: Boolean = false,
    cbcEligibility: CardBrandChoiceEligibility = CardBrandChoiceEligibility.Ineligible,
    cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
    cardFundingFilter: CardFundingFilter = DefaultCardFundingFilter,
    val cardDetailsAction: CardDetailsAction? = null,
    private val automaticallyLaunchedCardScanFormDataHelper: AutomaticallyLaunchedCardScanFormDataHelper?,
) : SectionFieldValidationController {

    internal val cardDetailsElement = CardDetailsElement(
        IdentifierSpec.Generic("card_detail"),
        cardAccountRangeRepositoryFactory,
        initialValues,
        collectName,
        cbcEligibility,
        cardBrandFilter,
        cardFundingFilter,
    )

    fun shouldAutomaticallyLaunchCardScan(): Boolean {
        return cardDetailsAction == null &&
            automaticallyLaunchedCardScanFormDataHelper?.shouldLaunchCardScanAutomatically == true
    }

    fun setHasAutomaticallyLaunchedCardScan() {
        this.automaticallyLaunchedCardScanFormDataHelper?.let {
            it.hasAutomaticallyLaunchedCardScan = true
        }
    }

    override val validationMessage = cardDetailsElement.controller.validationMessage

    override fun onValidationStateChanged(isValidating: Boolean) {
        cardDetailsElement.onValidationStateChanged(isValidating)
    }

    internal fun onCardScanResult(cardScanResult: CardScanResult) {
        cardDetailsElement.controller.onCardScanResult.invoke(cardScanResult)
    }
}
