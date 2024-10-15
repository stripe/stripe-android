package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.core.strings.ResolvableString
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
    private val cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
    override val identifier: IdentifierSpec,
    override val controller: CardDetailsSectionController = CardDetailsSectionController(
        cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
        initialValues = initialValues,
        collectName = collectName,
        cbcEligibility = cbcEligibility,
        cardBrandFilter = cardBrandFilter
    ),
) : FormElement {
    override val allowsUserInteraction: Boolean = true
    override val mandateText: ResolvableString? = null

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        controller.cardDetailsElement.getFormFieldValueFlow()

    override fun getTextFieldIdentifiers(): StateFlow<List<IdentifierSpec>> =
        controller.cardDetailsElement.getTextFieldIdentifiers()
}
