package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class RowController(
    val fields: List<SectionSingleFieldElement>
) : SectionFieldErrorController {

    @ExperimentalCoroutinesApi
    override val error = combine(
        fields.map { it.sectionFieldErrorController().error }
    ) {
        it.filterNotNull().firstOrNull()
    }
}
