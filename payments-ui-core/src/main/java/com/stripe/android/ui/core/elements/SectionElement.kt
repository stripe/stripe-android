package com.stripe.android.paymentsheet.elements

import com.stripe.android.paymentsheet.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal data class SectionElement(
    override val identifier: IdentifierSpec,
    val fields: List<SectionFieldElement>,
    override val controller: SectionController
) : FormElement() {
    internal constructor(
        identifier: IdentifierSpec,
        field: SectionFieldElement,
        controller: SectionController
    ) : this(identifier, listOf(field), controller)

    override fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        combine(fields.map { it.getFormFieldValueFlow() }) {
            it.toList().flatten()
        }
}
