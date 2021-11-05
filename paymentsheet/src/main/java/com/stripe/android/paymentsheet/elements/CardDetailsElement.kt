package com.stripe.android.paymentsheet.elements

import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import kotlinx.coroutines.flow.combine

internal class CardDetailsElement(
    identifier: IdentifierSpec,
    val controller: CardController = CardController(),
) : SectionMultiFieldElement(identifier) {

    /**
     * This will return a controller that abides by the SectionFieldErrorController interface.
     */
    override fun sectionFieldErrorController(): SectionFieldErrorController =
        controller

    override fun setRawValue(formFragmentArguments: FormFragmentArguments) {
        TODO("Not yet implemented")
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
