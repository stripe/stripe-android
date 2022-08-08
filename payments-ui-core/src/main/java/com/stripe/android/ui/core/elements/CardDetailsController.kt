package com.stripe.android.ui.core.elements

import android.content.Context
import kotlinx.coroutines.flow.combine
import java.util.UUID

internal class CardDetailsController constructor(
    context: Context,
    initialValues: Map<IdentifierSpec, String?>,
    cardNumberReadOnly: Boolean = false
) : SectionFieldErrorController {

    val label: Int? = null
    val numberElement = CardNumberElement(
        IdentifierSpec.CardNumber,
        if (cardNumberReadOnly) {
            CardNumberViewOnlyController(
                CardNumberConfig(),
                initialValues
            )
        } else {
            CardNumberEditableController(
                CardNumberConfig(),
                context,
                initialValues[IdentifierSpec.CardNumber]
            )
        }
    )

    val cvcElement = CvcElement(
        IdentifierSpec.CardCvc,
        CvcController(
            CvcConfig(),
            numberElement.controller.cardBrandFlow,
            initialValue = initialValues[IdentifierSpec.CardCvc]
        )
    )

    val expirationDateElement = SimpleTextElement(
        IdentifierSpec.Generic("date"),
        SimpleTextFieldController(
            DateConfig(),
            initialValue = initialValues[IdentifierSpec.CardExpMonth] +
                initialValues[IdentifierSpec.CardExpYear]?.takeLast(2)
        )
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

    override val error = combine(
        listOf(numberElement, expirationDateElement, cvcElement)
            .map { it.controller }
            .map { it.error }
    ) {
        it.filterNotNull().firstOrNull()
    }
}
