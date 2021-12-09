package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * This is the controller for a section with a static number of fields.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SectionController(
    @StringRes val label: Int?,
    val sectionFieldErrorControllers: List<SectionFieldErrorController>
) : Controller {
    val error: Flow<FieldError?> = combine(
        sectionFieldErrorControllers.map {
            it.error
        }
    ) {
        it.firstOrNull()
    }
}
