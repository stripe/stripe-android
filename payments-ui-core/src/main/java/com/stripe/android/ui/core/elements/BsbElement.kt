package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.elements.Controller
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.view.BecsDebitBanks

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BsbElement(
    private val identifierSpec: IdentifierSpec,
    private val banks: List<BecsDebitBanks.Bank>,
    initialValue: String?
) : FormElement {
    override val controller: Controller?
        get() = null
    override val identifier: IdentifierSpec
        get() = identifierSpec
    override val allowsUserInteraction: Boolean = true
    override val mandateText: ResolvableString? = null

    internal val textElement: SimpleTextElement = SimpleTextElement(
        identifier = IdentifierSpec.Generic("au_becs_debit[bsb_number]"),
        SimpleTextFieldController(BsbConfig(banks), initialValue = initialValue)
    )

    val bankName = textElement.controller.fieldValue.mapAsStateFlow { textFieldValue ->
        banks
            .filter { textFieldValue.startsWith(it.prefix) }
            .map { it.name }
            .firstOrNull()
    }

    override fun getFormFieldValueFlow() = combineAsStateFlow(
        textElement.controller.isComplete,
        textElement.controller.fieldValue
    ) { complete, fieldValue ->
        listOf(
            identifier to FormFieldEntry(fieldValue, complete)
        )
    }
}
