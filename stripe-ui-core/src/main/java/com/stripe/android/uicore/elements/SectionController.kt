package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * This is the controller for a section with a static number of fields.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SectionController(
    val label: ResolvableString?,
    sectionFieldValidationControllers: List<SectionFieldValidationController>
) : Controller {
    val validationMessage: StateFlow<FieldValidationMessage?> = combineAsStateFlow(
        sectionFieldValidationControllers.map {
            it.validationMessage
        }
    ) { validationMessageArray ->
        validationMessageArray.firstNotNullOfOrNull { it }
    }
}
