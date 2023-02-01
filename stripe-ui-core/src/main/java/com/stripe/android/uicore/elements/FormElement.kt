package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * This is used to define each section in the visual form layout.
 * Each item in the layout has an identifier and a controller associated with it.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface FormElement {
    val identifier: IdentifierSpec
    val controller: Controller?

    fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>>
    fun getTextFieldIdentifiers(): Flow<List<IdentifierSpec>> =
        MutableStateFlow(emptyList())
}
