package com.stripe.android.paymentsheet.elements

import com.stripe.android.paymentsheet.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow

/**
 * This class provides the logic behind the fields.
 */
internal sealed interface InputController : SectionFieldErrorController {
    val label: Int
    val fieldValue: Flow<String>
    val rawFieldValue: Flow<String?>
    val isComplete: Flow<Boolean>
    val showOptionalLabel: Boolean
    val formFieldValue: Flow<FormFieldEntry>

    fun onRawValueChange(rawValue: String)
}
