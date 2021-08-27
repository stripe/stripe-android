package com.stripe.android.paymentsheet.elements

import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.coroutines.flow.combine

internal class CreditDetailElement(
    identifier: IdentifierSpec,
    val controller: CreditElementController = CreditElementController(),
) : SectionMultiFieldElement(identifier) {

    /**
     * This will return a controller that abides by the SectionFieldErrorController interface.
     */
    override fun sectionFieldErrorController(): SectionFieldErrorController =
        controller

    override fun getFormFieldValueFlow() = combine(
        controller.numberElement.controller.formFieldValue,
        controller.cvcElement.controller.formFieldValue,
        controller.expirationDateElement.controller.formFieldValue
    ) { number, cvc, expirationDate ->
        listOf(
            controller.numberElement.identifier to number,
            controller.cvcElement.identifier to cvc,
            IdentifierSpec("month") to expirationDate.copy(
                value = expirationDate.value?.take(2)
            ),
            IdentifierSpec("year") to expirationDate.copy(
                value = expirationDate.value?.takeLast(2)
            )
        )
    }
}
