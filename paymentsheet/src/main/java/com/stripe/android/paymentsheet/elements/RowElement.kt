package com.stripe.android.paymentsheet.elements

import com.stripe.android.paymentsheet.SectionMultiFieldElement
import com.stripe.android.paymentsheet.SectionSingleFieldElement
import com.stripe.android.paymentsheet.forms.FormFieldEntry
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class RowElement constructor(
    _identifier: IdentifierSpec,
    val fields: List<SectionSingleFieldElement>,
    val controller: RowController
) : SectionMultiFieldElement(_identifier) {
    override fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        combine(fields.map { it.getFormFieldValueFlow() }) {
            it.toList().flatten()
        }

    override fun sectionFieldErrorController() = controller

    override fun setRawValue(formFragmentArguments: FormFragmentArguments) {
        fields.forEach {
            it.setRawValue(formFragmentArguments)
        }
    }
}
