package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.InputController
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

/**
 * This is an element that has static text because it takes no user input, it is not
 * outputted from the list of form field values.  If the stringResId contains a %s, the first
 * one will be populated in the form with the merchantName parameter.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class StaticTextElement(
    override val identifier: IdentifierSpec,
    val text: ResolvableString,
    override val controller: InputController? = null
) : FormElement {
    constructor(
        identifier: IdentifierSpec,
        stringResId: Int,
        controller: InputController? = null
    ) : this(
        identifier = identifier,
        text = stringResId.resolvableString,
        controller = controller,
    )

    override val allowsUserInteraction: Boolean = false
    override val mandateText: ResolvableString? = null

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        stateFlowOf(emptyList())
}
