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

internal class AddressSectionController(
    @StringRes val label: Int?,
    val sectionFieldControllers: Flow<List<InputController>>
) : Controller {
    val error: Flow<FieldError?> = MutableStateFlow(null)
}

/**
 * This class provides the logic behind the fields.
 */
internal sealed interface InputController : Controller {
    val label: Int
    val fieldValue: Flow<String>
    val rawFieldValue: Flow<String?>
    val isComplete: Flow<Boolean>
    val error: Flow<FieldError?>
    val elementType: ElementType

    fun onRawValueChange(rawValue: String)
}

internal data class FieldError(
    @StringRes val errorFieldLabel: Int,
    @StringRes val errorMessage: Int
)
