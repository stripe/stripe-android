package com.stripe.android.paymentsheet.elements

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.SectionFieldElement
import com.stripe.android.paymentsheet.forms.FormFieldEntry
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import com.stripe.android.viewmodel.credit.cvc.CvcConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine

internal class CreditElementController : SectionFieldErrorController {

    val label: Int? = null
    val numberElement = SectionFieldElement.CardNumberText(
        IdentifierSpec("number"),
        CreditNumberTextFieldController(CardNumberConfig())
    )

    val cvcElement = SectionFieldElement.CvcText(
        IdentifierSpec("cvc"),
        CvcTextFieldController(CvcConfig(), numberElement.controller.cardBrandFlow)
    )

    val expirationDateElement = SectionFieldElement.SimpleText(
        IdentifierSpec("date"),
        SimpleTextFieldController(
            SimpleTextFieldConfig(
                R.string.credit_expiration_date,
                KeyboardCapitalization.None,
                KeyboardType.Number
            )
        )
    )

    // TODO: add expiration date
    val fields = listOf(numberElement, cvcElement, expirationDateElement)

    @ExperimentalCoroutinesApi
    override val error = combine(fields
        .map { it.controller }
        .map { it.error }
    ) {
        it.filterNotNull().firstOrNull()
    }
}
