package com.stripe.android.paymentsheet.elements

import com.stripe.android.paymentsheet.SectionSingleFieldElement
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import com.stripe.android.viewmodel.credit.cvc.CvcConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine

internal class CreditElementController : SectionFieldErrorController {

    val label: Int? = null
    val numberElement = SectionSingleFieldElement.CardNumberText(
        IdentifierSpec("number"),
        CreditNumberTextFieldController(CardNumberConfig())
    )

    val cvcElement = SectionSingleFieldElement.CvcText(
        IdentifierSpec("cvc"),
        CvcTextFieldController(CvcConfig(), numberElement.controller.cardBrandFlow)
    )

    val expirationDateElement = SectionSingleFieldElement.SimpleText(
        IdentifierSpec("date"),
        SimpleTextFieldController(DateConfig())
    )

    // TODO: add expiration date
    val fields = listOf(numberElement, cvcElement, expirationDateElement)

    @ExperimentalCoroutinesApi
    override val error = combine(
        fields
            .map { it.controller }
            .map { it.error }
    ) {
        it.filterNotNull().firstOrNull()
    }
}
