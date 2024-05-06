package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

/**
 * This is the controller for a section with a static number of fields.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SectionController(
    @StringRes val label: Int?,
    sectionFieldErrorControllers: List<SectionFieldErrorController>
) : Controller {
    val error: StateFlow<FieldError?> = if (sectionFieldErrorControllers.isEmpty()) {
        stateFlowOf(null)
    } else {
        combineAsStateFlow(
            sectionFieldErrorControllers.map {
                it.error
            }
        ) { errorArray ->
            errorArray.firstNotNullOfOrNull { it }
        }
    }
}
