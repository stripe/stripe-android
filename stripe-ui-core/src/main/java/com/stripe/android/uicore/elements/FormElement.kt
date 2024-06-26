package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

/**
 * This is used to define each section in the visual form layout.
 * Each item in the layout has an identifier and a controller associated with it.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface FormElement {
    val identifier: IdentifierSpec
    val controller: Controller?

    /**
     * Whether the form element allows user interaction.
     *
     * This is used to determine if vertical mode needs to show the form screen.
     */
    val allowsUserInteraction: Boolean

    /**
     * The mandate text, if any.
     *
     * This is used to determine if vertical mode needs to show mandates for LPMs with no fields, only a mandate.
     */
    val mandateText: ResolvableString?

    fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>>
    fun getTextFieldIdentifiers(): StateFlow<List<IdentifierSpec>> =
        stateFlowOf(emptyList())
}
