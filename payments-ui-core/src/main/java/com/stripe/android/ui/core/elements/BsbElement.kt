package com.stripe.android.ui.core.elements

import com.stripe.android.ui.core.forms.FormFieldEntry
import com.stripe.android.view.BecsDebitBanks
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class BsbElement(
    private val identifierSpec: IdentifierSpec,
    private val banks: List<BecsDebitBanks.Bank>
) : FormElement() {
    override val controller: Controller?
        get() = null
    override val identifier: IdentifierSpec
        get() = identifierSpec

    val textElement: SimpleTextElement = SimpleTextElement(
        identifier = IdentifierSpec.Generic("bsb_number"),
        TextFieldController(BsbConfig(banks))
    )

    val bankName = textElement.controller.fieldValue.map { textFieldValue ->
        if (textFieldValue.length >= 2) {
            banks
                .filter { it.prefix == textFieldValue.substring(0, 2) }
                .map { it.name }
                .firstOrNull()
        } else {
            null
        }
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
