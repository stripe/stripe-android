package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.Flow

/** This is a generic controller */
internal sealed interface Controller

/**
 * Any element in a section must have a controller that provides
 * an error and have a type.
 */
internal sealed interface SectionFieldController : Controller {
    val error: Flow<FieldError?>
}

/**
 * This class provides the logic behind the fields.
 */
internal sealed interface InputController : SectionFieldController {
    val label: Int
    val fieldValue: Flow<String>
    val rawFieldValue: Flow<String?>
    val isComplete: Flow<Boolean>
    override val error: Flow<FieldError?>

    fun onRawValueChange(rawValue: String)
}

internal data class FieldError(
    @StringRes val errorFieldLabel: Int,
    @StringRes val errorMessage: Int
)
