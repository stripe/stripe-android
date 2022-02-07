package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.Flow

/**
 * Any element in a section must have a controller that provides
 * an error and have a type.  This is used for a single field in a section
 * or a section field that has other fields in it.
 */
internal sealed interface SectionFieldErrorController : Controller {
    val error: Flow<FieldError?>
}

/**
 * Encapsulates an error message including the string resource and the variable arguments
 */
internal class FieldError(
    @StringRes val errorMessage: Int,
    val formatArgs: Array<out Any>? = null
)
