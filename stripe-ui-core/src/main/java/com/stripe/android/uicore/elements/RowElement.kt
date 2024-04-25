package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class RowElement constructor(
    _identifier: IdentifierSpec,
    val fields: List<SectionSingleFieldElement>,
    val controller: RowController
) : SectionMultiFieldElement(_identifier) {
    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        combineAsStateFlow(fields.map { it.getFormFieldValueFlow() }) {
            it.toList().flatten()
        }

    override fun sectionFieldErrorController() = controller

    override fun setRawValue(rawValuesMap: Map<IdentifierSpec, String?>) {
        fields.forEach {
            it.setRawValue(rawValuesMap)
        }
    }

    override fun getTextFieldIdentifiers(): StateFlow<List<IdentifierSpec>> =
        fields.map { it.getTextFieldIdentifiers() }.last()
}
