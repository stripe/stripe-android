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
        val month = ""
        if (expirationDate.value?.getOrNull(0) == '0' || expirationDate.value?.getOrNull(0) == '1') {
            value = expirationDate.value?.take(2)

        }

        listOf(
            controller.numberElement.identifier to number,
            controller.cvcElement.identifier to cvc,
            IdentifierSpec.Generic("exp_month") to expirationDate.copy(
                value = expirationDate.value?.take(2)
            ),
            IdentifierSpec.Generic("exp_year") to expirationDate.copy(
                value = expirationDate.value?.takeLast(2)
            )
        )
    }
}
