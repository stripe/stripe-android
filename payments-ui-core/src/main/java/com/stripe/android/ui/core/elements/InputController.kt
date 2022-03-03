package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow

/**
 * This class provides the logic behind the fields.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface InputController : SectionFieldErrorController {
    val label: Int?
    val fieldValue: Flow<String>
    val rawFieldValue: Flow<String?>
    val isComplete: Flow<Boolean>
    val showOptionalLabel: Boolean
    val formFieldValue: Flow<FormFieldEntry>

    fun onRawValueChange(rawValue: String)
}
