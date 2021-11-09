package com.stripe.android.paymentsheet.elements

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import java.util.UUID

internal class CardController() : SectionFieldErrorController {

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

    private val rowFields = listOf(expirationDateElement, cvcElement)
    val fields = listOf(
        numberElement,
        RowElement(
            IdentifierSpec.Generic("row_" + UUID.randomUUID().leastSignificantBits),
            rowFields,
            RowController(rowFields)
        )
    )

    @ExperimentalCoroutinesApi
    override val error = combine(
        listOf(numberElement, expirationDateElement, cvcElement)
            .map { it.controller }
            .map { it.error }
    ) {
        it.filterNotNull().firstOrNull()
    }
}
