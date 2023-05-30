package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class OTPController(val otpLength: Int = 6) : Controller {
    internal val keyboardType = KeyboardType.NumberPassword

    internal val fieldValues: List<MutableStateFlow<String>> = (0 until otpLength).map {
        MutableStateFlow("")
    }

    val fieldValue: Flow<String> = combine(fieldValues) {
        it.joinToString("")
    }.distinctUntilChanged()

    /**
     * Filter invalid values and set the value of the fields to the entered text, one character per
     * field starting from [index].
     * If the length of the filtered input is the same as the OTP we're collecting, set the full
     * input value regardless of the starting index passed as parameter.
     *
     * @return the number of fields that had their values set
     */
    fun onValueChanged(index: Int, text: String): Int {
        if (text == fieldValues[index].value) {
            return 1
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

    fun reset() {
        fieldValues.forEach { it.value = "" }
    }

    private fun filter(userTyped: String) = userTyped.filter { VALID_INPUT_RANGES.contains(it) }

    private companion object {
        val VALID_INPUT_RANGES = ('0'..'9')
    }
}
