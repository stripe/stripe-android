package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import com.stripe.android.paymentsheet.ElementType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** This is a generic controller */
internal sealed interface Controller

/**
 * Any element in a section must have a controller that provides
 * an error and have a type.
 */
internal sealed interface SectionFieldController : Controller{
    val error: Flow<FieldError?>
    val elementType: ElementType
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
    override val elementType: ElementType

    fun onRawValueChange(rawValue: String)
}

internal data class FieldError(
    @StringRes val errorFieldLabel: Int,
    @StringRes val errorMessage: Int
)
