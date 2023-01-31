package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.InputController
import com.stripe.android.uicore.elements.SectionSingleFieldElement
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.forms.FormFieldEntry
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
