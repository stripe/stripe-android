package com.stripe.android.paymentsheet.elements

import com.stripe.android.paymentsheet.ElementType
import kotlinx.coroutines.flow.Flow

/**
 * This class provides the logic behind the fields.
 */
internal sealed interface InputController {
    val label: Int
    val fieldValue: Flow<String>
    val rawFieldValue: Flow<String?>
    val isComplete: Flow<Boolean>
    val errorMessage: Flow<Int?>
    val elementType: ElementType

    fun onRawValueChange(rawValue: String)
}
