package com.stripe.android.paymentsheet.elements

import com.stripe.android.paymentsheet.SectionFieldElement
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import com.stripe.android.viewmodel.credit.cvc.CvcConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine

internal class CreditController(
) : SectionFieldErrorController {

    val numberElement = SectionFieldElement.CardNumberText(
        IdentifierSpec("number"),
        CreditNumberTextFieldController(CardNumberConfig())
    )

    val cvcElement = SectionFieldElement.CvcText(
        IdentifierSpec("cvc"),
        CvcTextFieldController(CvcConfig(), numberElement.controller.cardBrandFlow)
    )

    // TODO: add expiration date
    val fields = listOf(cvcElement, numberElement)

    @ExperimentalCoroutinesApi
    override val error = combine(fields
        .map { it.controller }
        .map { it.error }
    ) {
        it.firstOrNull()
    }
}
