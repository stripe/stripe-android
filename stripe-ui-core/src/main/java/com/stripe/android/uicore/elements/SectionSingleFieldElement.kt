package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * This is an element that is in a section and accepts user input.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class SectionSingleFieldElement(
    override val identifier: IdentifierSpec
) : SectionFieldElement {
    /**
     * Some fields in the section will have a single input controller.
     */
    abstract val controller: InputController

    /**
     * This will return a controller that abides by the SectionFieldErrorController interface.
     */
    override fun sectionFieldErrorController(): SectionFieldErrorController = controller

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> {
        return controller.formFieldValue.mapAsStateFlow { formFieldEntry ->
            listOf(identifier to formFieldEntry)
        }
    }

    override fun setRawValue(rawValuesMap: Map<IdentifierSpec, String?>) {
        rawValuesMap[identifier]?.let { controller.onRawValueChange(it) }
    }

    override fun getTextFieldIdentifiers(): StateFlow<List<IdentifierSpec>> =
        MutableStateFlow(
            listOf(identifier).takeIf { controller is TextFieldController }
                ?: emptyList()
        )
}
