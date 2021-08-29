package com.stripe.android.paymentsheet.elements

import com.stripe.android.viewmodel.credit.cvc.CvcConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine

internal class CardController : SectionFieldErrorController {

    val label: Int? = null
    val numberElement = CardNumberElement(
        IdentifierSpec.Generic("number"),
        CardNumberController(CardNumberConfig())
    )

    val cvcElement = CvcElement(
        IdentifierSpec.Generic("cvc"),
        CvcController(CvcConfig(), numberElement.controller.cardBrandFlow)
    )

    val expirationDateElement = SimpleTextElement(
        IdentifierSpec.Generic("date"),
        SimpleTextFieldController(DateConfig())
    )

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
