package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class OTPController(
    val otpLength: Int = 6,
) : Controller {
    val textFieldControllers = (0 until otpLength).map {
        TextFieldController(OTPCellConfig())
    }

    val fieldValue: Flow<List<String>> = combine(textFieldControllers.map { it.fieldValue }) {
        it.toList()
    }

    val rawFieldValue: Flow<String> = fieldValue.map {
        it.joinToString("")
    }

    fun onDelete(index: Int) {
        textFieldControllers[index].onValueChange("")
    }
}
