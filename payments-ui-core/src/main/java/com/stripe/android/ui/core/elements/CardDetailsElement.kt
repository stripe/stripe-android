package com.stripe.android.ui.core.elements

import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.model.CardBrand
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionFieldErrorController
import com.stripe.android.uicore.elements.SectionMultiFieldElement
import com.stripe.android.uicore.elements.convertTo4DigitDate
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

/**
 * This is the element that represent the collection of all the card details:
 * card number, expiration date, and CVC.
 */
internal class CardDetailsElement(
    identifier: IdentifierSpec,
    cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    initialValues: Map<IdentifierSpec, String?>,
    collectName: Boolean = false,
    private val cbcEligibility: CardBrandChoiceEligibility = CardBrandChoiceEligibility.Ineligible,
    val controller: CardDetailsController = CardDetailsController(
        cardAccountRangeRepositoryFactory,
        initialValues,
        collectName,
        cbcEligibility,
    )
) : SectionMultiFieldElement(identifier) {
    val isCardScanEnabled = controller.numberElement.controller.cardScanEnabled

    override fun sectionFieldErrorController(): SectionFieldErrorController =
        controller

    override fun setRawValue(rawValuesMap: Map<IdentifierSpec, String?>) {
        // Nothing from FormArguments to populate
    }

    override fun getTextFieldIdentifiers(): StateFlow<List<IdentifierSpec>> =
        stateFlowOf(
            listOfNotNull(
                controller.nameElement?.identifier,
                controller.numberElement.identifier,
                controller.expirationDateElement.identifier,
                controller.cvcElement.identifier
            )
        )

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> {
        val flows = buildList {
            if (controller.nameElement != null) {
                add(
                    controller.nameElement.controller.formFieldValue.mapAsStateFlow {
                        controller.nameElement.identifier to it
                    }
                )
            }
            add(
                controller.numberElement.controller.formFieldValue.mapAsStateFlow {
                    controller.numberElement.identifier to it
                }
            )
            add(
                controller.cvcElement.controller.formFieldValue.mapAsStateFlow {
                    controller.cvcElement.identifier to it
                }
            )
            add(
                controller.numberElement.controller.cardBrandFlow.mapAsStateFlow {
                    IdentifierSpec.CardBrand to FormFieldEntry(it.code, true)
                }
            )
            if (cbcEligibility is CardBrandChoiceEligibility.Eligible) {
                add(
                    controller.numberElement.controller.selectedCardBrandFlow.mapAsStateFlow { brand ->
                        IdentifierSpec.PreferredCardBrand to FormFieldEntry(
                            value = brand.code.takeUnless { brand == CardBrand.Unknown },
                            isComplete = true
                        )
                    }
                )
            }
            add(
                controller.expirationDateElement.controller.formFieldValue.mapAsStateFlow {
                    IdentifierSpec.CardExpMonth to getExpiryMonthFormFieldEntry(it)
                }
            )
            add(
                controller.expirationDateElement.controller.formFieldValue.mapAsStateFlow {
                    IdentifierSpec.CardExpYear to getExpiryYearFormFieldEntry(it)
                }
            )
        }
        return combineAsStateFlow(flows) { it.toList() }
    }
}

private fun getExpiryMonthFormFieldEntry(entry: FormFieldEntry): FormFieldEntry {
    var month = -1
    entry.value?.let { date ->
        val newString = convertTo4DigitDate(date)
        if (newString.length == 4) {
            month = requireNotNull(newString.take(2).toIntOrNull())
        }
    }

    return entry.copy(
        value = month.toString().padStart(length = 2, padChar = '0')
    )
}

private fun getExpiryYearFormFieldEntry(entry: FormFieldEntry): FormFieldEntry {
    var year = -1
    entry.value?.let { date ->
        val newString = convertTo4DigitDate(date)
        if (newString.length == 4) {
            year = requireNotNull(newString.takeLast(2).toIntOrNull()) + 2000
        }
    }

    return entry.copy(
        value = year.toString()
    )
}
