package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class OTPController(val otpLength: Int = 6) : Controller {
    internal val keyboardType = KeyboardType.Number

    internal val fieldValues: List<MutableStateFlow<String>> = (0 until otpLength).map {
        MutableStateFlow("")
    }

    val fieldValue: Flow<String> = combine(fieldValues) {
        it.joinToString("")
    }

    fun onValueChanged(index: Int, text: String): Int {
        if (text == fieldValues[index].value) {
            return 0
        }

        if (text.isEmpty()) {
            fieldValues[index].value = ""
            return 0
        }

        val filtered = filter(text)

        // If the user is pasting a value that matches the length of the OTP, assume they want to
        // use this value as the full input, regardless of which TextField currently has focus.
        val offset = if (filtered.length == otpLength) 0 else index
        val inputLength = minOf(otpLength, filtered.length)

        (0 until inputLength).forEach {
            fieldValues[offset + it].value = filtered[it].toString()
        }

        return inputLength
    }

    private fun filter(userTyped: String) = userTyped.filter { VALID_INPUT_RANGES.contains(it) }

    private companion object {
        val VALID_INPUT_RANGES = ('0'..'9')
    }
}
