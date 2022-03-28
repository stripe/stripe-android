package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

/**
 * This is the controller for a section with a changing number and set of fields.
 * This is in contrast to the [SectionController] which is a section in which the fields
 * in it do not change.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AddressController(
    val fieldsFlowable: Flow<List<SectionFieldElement>>
) : SectionFieldErrorController {
    @StringRes
    val label: Int? = null

    override val error = fieldsFlowable.flatMapLatest { sectionFieldElements ->
        combine(
            sectionFieldElements.map { it.sectionFieldErrorController().error }
        ) { fieldErrors ->
            fieldErrors.filterNotNull().firstOrNull()
        }
    }
}
