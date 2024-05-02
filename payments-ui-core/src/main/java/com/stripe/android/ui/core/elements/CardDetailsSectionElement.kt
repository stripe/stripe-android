package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CardDetailsSectionElement(
    cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    initialValues: Map<IdentifierSpec, String?>,
    private val collectName: Boolean = false,
    private val cbcEligibility: CardBrandChoiceEligibility = CardBrandChoiceEligibility.Ineligible,
    override val identifier: IdentifierSpec,
    override val controller: CardDetailsSectionController = CardDetailsSectionController(
        cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
        initialValues = initialValues,
        collectName = collectName,
        cbcEligibility = cbcEligibility,
    ),
) : FormElement {
    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        controller.cardDetailsElement.getFormFieldValueFlow()

    override fun getTextFieldIdentifiers(): StateFlow<List<IdentifierSpec>> =
        controller.cardDetailsElement.getTextFieldIdentifiers()
}
