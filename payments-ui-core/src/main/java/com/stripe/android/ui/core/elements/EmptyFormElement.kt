package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.Controller
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class EmptyFormElement(
    override val identifier: IdentifierSpec = IdentifierSpec.Generic("empty_form"),
    override val controller: Controller? = null
) : FormElement {
    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        stateFlowOf(emptyList())
}
