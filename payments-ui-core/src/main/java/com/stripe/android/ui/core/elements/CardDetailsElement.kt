package com.stripe.android.ui.core.elements

import kotlinx.coroutines.flow.combine

/**
 * This is the element that represent the collection of all the card details:
 * card number, expiration date, and CVC.
 */
internal class CardDetailsElement(
    identifier: IdentifierSpec,
    val controller: CardDetailsController = CardDetailsController(),
) : SectionMultiFieldElement(identifier) {
    override fun sectionFieldErrorController(): SectionFieldErrorController =
        controller

    override fun setRawValue(rawValuesMap: Map<IdentifierSpec, String?>) {
        // Nothing from formFragmentArguments to populate
    }

    override fun getFormFieldValueFlow() = combine(
        controller.numberElement.controller.formFieldValue,
        controller.cvcElement.controller.formFieldValue,
        controller.expirationDateElement.controller.formFieldValue
    ) { number, cvc, expirationDate ->
        var month = -1
        var year = -1
        expirationDate.value?.let { date ->
            val newString = convertTo4DigitDate(date)
            if (newString.length == 4) {
                month = requireNotNull(newString.take(2).toIntOrNull())
                year = requireNotNull(newString.takeLast(2).toIntOrNull())
            }
        }

        listOf(
            controller.numberElement.identifier to number,
            controller.cvcElement.identifier to cvc,
            IdentifierSpec.Generic("exp_month") to expirationDate.copy(
                value = month.toString()
            ),
            IdentifierSpec.Generic("exp_year") to expirationDate.copy(
                value = year.toString()
            )
        )
    }
}
