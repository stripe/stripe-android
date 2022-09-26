package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class UpiElement(
    override val identifier: IdentifierSpec,
    override val controller: InputController = SimpleTextFieldController(
        textFieldConfig = UpiConfig()
    )
) : SectionSingleFieldElement(identifier = identifier) {

    override fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>> {
        return controller.formFieldValue.map { entry ->
            listOf(identifier to entry)
        }
    }
}
