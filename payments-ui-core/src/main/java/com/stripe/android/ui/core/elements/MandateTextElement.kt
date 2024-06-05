package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.InputController
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class MandateTextElement(
    override val identifier: IdentifierSpec,
    val stringResId: Int,
    val args: List<String>,
    override val controller: InputController? = null
) : FormElement {
    override val allowsUserInteraction: Boolean = false

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        stateFlowOf(emptyList())
}
