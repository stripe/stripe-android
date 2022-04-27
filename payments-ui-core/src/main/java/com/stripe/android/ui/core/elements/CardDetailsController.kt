package com.stripe.android.ui.core.elements

import android.content.Context
import kotlinx.coroutines.flow.combine
import java.util.UUID

internal class CardDetailsController constructor(
    context: Context,
    initialValues: Map<IdentifierSpec, String?>
) : SectionFieldErrorController {

    val label: Int? = null
    val numberElement = CardNumberElement(
        IdentifierSpec.CardNumber,
        CardNumberController(
            CardNumberConfig(),
            context,
            initialValue = initialValues[IdentifierSpec.CardNumber]
        )
    )

    val cvcElement = CvcElement(
        IdentifierSpec.Generic("card[cvc]"),
        CvcController(
            CvcConfig(),
            numberElement.controller.cardBrandFlow,
            initialValue = initialValues[IdentifierSpec.Generic("card[cvc]")]
        )
    )

    val expirationDateElement = SimpleTextElement(
        IdentifierSpec.Generic("date"),
        SimpleTextFieldController(
            DateConfig(),
            initialValue = initialValues[IdentifierSpec.Generic("card[exp_month]")] +
                initialValues[IdentifierSpec.Generic("card[exp_year]")]?.takeLast(2)
        ),
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
