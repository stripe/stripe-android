package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface SectionFieldElement {
    val identifier: IdentifierSpec
    val shouldRenderOutsideCard: Boolean
        get() = false

    /**
     * Whether the field element allows user interaction.
     *
     * This is used to determine if vertical mode needs to show the form screen.
     */
    val allowsUserInteraction: Boolean

    fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>>
    fun sectionFieldErrorController(): SectionFieldErrorController
    fun setRawValue(rawValuesMap: Map<IdentifierSpec, String?>)
    fun getTextFieldIdentifiers(): StateFlow<List<IdentifierSpec>>
}
