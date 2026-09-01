package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.elements.TextFieldController
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
    constructor(
        identifierSpec: IdentifierSpec,
        initialValue: String?,
    ) : this(
        identifierSpec = identifierSpec,
        banks = auBecsDebitBanks,
        initialValue = initialValue,
    )

    override val controller: TextFieldController = SimpleTextFieldController(
        textFieldConfig = BsbConfig(banks),
        initialValue = initialValue,
    )
    override val identifier: IdentifierSpec
        get() = identifierSpec
    override val allowsUserInteraction: Boolean = true
    override val mandateText: ResolvableString? = null

    val bankName = controller.fieldValue.mapAsStateFlow { textFieldValue ->
        banks
            .filter { textFieldValue.startsWith(it.prefix) }
            .map { it.name }
            .firstOrNull()
    }

    override fun getFormFieldValueFlow() = combineAsStateFlow(
        controller.isComplete,
        controller.fieldValue
    ) { complete, fieldValue ->
        listOf(
            identifier to FormFieldEntry(fieldValue, complete)
        )
    }
}
