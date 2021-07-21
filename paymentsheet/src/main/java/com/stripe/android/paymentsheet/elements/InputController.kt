package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.Flow

/** This is a generic controller */
internal sealed interface Controller

/**
 * This class provides the logic behind the fields.
 */
internal sealed interface InputController : Controller {
    val label: Int
    val fieldValue: Flow<String>
    val rawFieldValue: Flow<String?>
    val isComplete: Flow<Boolean>
    val error: Flow<FieldError?>

    fun onRawValueChange(rawValue: String)
}

/**
 * Encapsulates an error message including the string resource and the variable arguments
 */
internal class FieldError(
    @StringRes val errorMessage: Int,
    val formatArgs: Array<out Any>? = null
)
