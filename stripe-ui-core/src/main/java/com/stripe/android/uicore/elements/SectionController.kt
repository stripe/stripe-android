package com.stripe.android.uicore.elements

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
    sectionFieldErrorControllers: List<SectionFieldErrorController>
) : Controller {
    val error: Flow<FieldError?> = combine(
        sectionFieldErrorControllers.map {
            it.error
        }
    ) { errorArray ->
        errorArray.firstNotNullOfOrNull { it }
    }
}
