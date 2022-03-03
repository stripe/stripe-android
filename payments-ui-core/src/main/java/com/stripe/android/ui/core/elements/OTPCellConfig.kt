package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class OTPCellConfig : TextFieldConfig {
    private var currentValue = ""

    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.None
    override val debugLabel = "OTP"

    @StringRes
    override val label: Int? = null
    override val keyboard = KeyboardType.Number
    override val visualTransformation: VisualTransformation? = null

    // Prevent the callback from being called twice when updating the field programmatically
    private var valueChangedCallbackEnabled = true

    override fun filter(userTyped: String): String {
        val filtered = userTyped.filter { VALID_INPUT_RANGES.contains(it) }
        if (valueChangedCallbackEnabled) {
            valueChangedCallbackEnabled = false
            if (filtered.length == 2) {
                val val1 = filtered.getOrNull(0)?.toString() ?: ""
                val val2 = filtered.getOrNull(1)?.toString() ?: ""
                currentValue = if (currentValue == val1) val2 else val1
                return currentValue
            } else {
                currentValue = filtered
            }
        }

        valueChangedCallbackEnabled = true
        return currentValue
    }

    override fun convertToRaw(displayName: String) = displayName

    override fun convertFromRaw(rawValue: String) = rawValue

    override fun determineState(input: String): TextFieldState {
        return when {
            input.isEmpty() -> TextFieldStateConstants.Error.Blank
            else -> TextFieldStateConstants.Valid.Full
        }
    }

    private companion object {
        val VALID_INPUT_RANGES = ('0'..'9')
    }
}
