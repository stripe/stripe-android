package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.InputController
import com.stripe.android.uicore.elements.SectionSingleFieldElement
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class BlikElement(
    override val identifier: IdentifierSpec = IdentifierSpec.BlikCode,
    override val controller: InputController = SimpleTextFieldController(
        textFieldConfig = BlikConfig()
    )
) : SectionSingleFieldElement(identifier = identifier) {
    override val allowsUserInteraction: Boolean = true
    override val mandateText: ResolvableString? = null

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> {
        return controller.formFieldValue.mapAsStateFlow { entry ->
            listOf(identifier to entry)
        }
    }
}
