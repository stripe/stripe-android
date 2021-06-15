package com.stripe.android.paymentsheet.elements.common

import kotlinx.coroutines.flow.Flow

/**
 * This class provides the logic behind the fields.
 */
interface Element {
    // A couple word description of the field.
    val label: Int
    // The value of the field.
    val fieldValue: Flow<String>
    // Indicates if the field is complete .
    val isComplete: Flow<Boolean>
    // Resource id indicating the error message of the field.
    val errorMessage: Flow<Int?>
}