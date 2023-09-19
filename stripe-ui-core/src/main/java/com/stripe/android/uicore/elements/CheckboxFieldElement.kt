package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.map

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CheckboxFieldElement(
    override val identifier: IdentifierSpec,
    override val controller: CheckboxFieldController = CheckboxFieldController()
) : FormElement {
    override fun getFormFieldValueFlow() = controller.isChecked.map { isChecked ->
        listOf(
            identifier to FormFieldEntry(isChecked.toString(), isChecked)
        )
    }
}
