package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import com.stripe.android.paymentsheet.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow

/** This is a generic controller */
internal sealed interface Controller

/**
 * Any element in a section must have a controller that provides
 * an error and have a type.  This is used for a single field in a section
 * or a section field that has other fields in it.
 */
internal sealed interface SectionFieldErrorController : Controller {
    val error: Flow<FieldError?>
}

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

/**
 * Encapsulates an error message including the string resource and the variable arguments
 */
internal class FieldError(
    @StringRes val errorMessage: Int,
    val formatArgs: Array<out Any>? = null
)
