package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.forms.FormFieldEntry
import com.stripe.android.view.BecsDebitBanks
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BsbElement(
    private val identifierSpec: IdentifierSpec,
    private val banks: List<BecsDebitBanks.Bank>
) : FormElement() {
    override val controller: Controller?
        get() = null
    override val identifier: IdentifierSpec
        get() = identifierSpec

    internal val textElement: SimpleTextElement = SimpleTextElement(
        identifier = IdentifierSpec.Generic("au_becs_debit[bsb_number]"),
        SimpleTextFieldController(BsbConfig(banks))
    )

    val bankName = textElement.controller.fieldValue.map { textFieldValue ->
        banks
            .filter { textFieldValue.startsWith(it.prefix) }
            .map { it.name }
            .firstOrNull()
    }

    override fun getFormFieldValueFlow() = combine(
        textElement.controller.isComplete,
        textElement.controller.fieldValue
    ) { complete, fieldValue ->
        listOf(
            identifier to FormFieldEntry(fieldValue, complete)
        )
    }
}
