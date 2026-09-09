package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.view.BecsDebitBanks

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BsbElement(
    private val formFieldId: FormFieldId,
    initialValue: String?
) : FormElement {
    private val banks = BecsDebitBanks()

    override val controller: TextFieldController = SimpleTextFieldController(
        textFieldConfig = BsbConfig(banks),
        initialValue = initialValue,
    )
    override val identifier: FormFieldId
        get() = formFieldId
    override val allowsUserInteraction: Boolean = true
    override val mandateText: ResolvableString? = null

    val bankName = controller.fieldValue.mapAsStateFlow { textFieldValue ->
        banks.byPrefix(textFieldValue)?.name
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
