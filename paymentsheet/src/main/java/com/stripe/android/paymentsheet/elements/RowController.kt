package com.stripe.android.paymentsheet.elements

import com.stripe.android.paymentsheet.SectionSingleFieldElement
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine

internal class RowController(
    val fields: List<SectionSingleFieldElement>
) : SectionFieldErrorController {

    @ExperimentalCoroutinesApi
    override val error = combine(
        fields.map { it.sectionFieldErrorController().error }
    ) {
        it.filterNotNull().firstOrNull()
    }
}
