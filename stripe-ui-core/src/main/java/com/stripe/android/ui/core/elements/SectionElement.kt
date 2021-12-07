package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.ui.core.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class SectionElement(
    override val identifier: IdentifierSpec,
    val fields: List<SectionFieldElement>,
    override val controller: SectionController
) : FormElement() {
    @VisibleForTesting
    constructor(
        identifier: IdentifierSpec,
        field: SectionFieldElement,
        controller: SectionController
    ) : this(identifier, listOf(field), controller)

    override fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        combine(fields.map { it.getFormFieldValueFlow() }) {
            it.toList().flatten()
        }

}
