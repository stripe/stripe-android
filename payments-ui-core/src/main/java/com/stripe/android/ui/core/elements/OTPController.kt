package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.core.text.isDigitsOnly
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class OTPController(
    val otpLength: Int = 6
) : Controller  {
    private val _fieldValue = MutableStateFlow((0 until otpLength).map { "" })
    val rawFieldValue: Flow<String> = _fieldValue.map {
        it.joinToString("")
    }
    val fieldValue: Flow<List<String>> = _fieldValue

    // Prevent the callback from being called twice when updating the field programmatically
    var valueChangedCallbackEnabled = true

    fun onValueChange(index: Int, value: String) {
        if (valueChangedCallbackEnabled && value.isDigitsOnly()) {
            valueChangedCallbackEnabled = false
            val newList = _fieldValue.value.toMutableList()
            if (value.length > 1) {
                val val1 = value.getOrNull(0)?.toString() ?: ""
                val val2 = value.getOrNull(1)?.toString() ?: ""
                val newValue = if (_fieldValue.value[index] == val1) val2 else val1
                newList[index] = newValue
                _fieldValue.value = newList
                return
            } else {
                newList[index] = value
            }
            _fieldValue.value = newList
        }
        valueChangedCallbackEnabled = true
    }
}
