package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.StateFlow

/**
 * This class provides the logic behind the fields.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface InputController : SectionFieldErrorController {
    val label: StateFlow<Int?>
    val fieldValue: StateFlow<String>
    val rawFieldValue: StateFlow<String?>
    val isComplete: StateFlow<Boolean>
    val showOptionalLabel: Boolean
    val formFieldValue: StateFlow<FormFieldEntry>

    fun onRawValueChange(rawValue: String)
}
