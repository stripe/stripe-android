package com.stripe.android.paymentsheet.elements

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType

internal class SimpleTextFieldConfig(
    override val label: Int,
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.Words,
    override val keyboard: KeyboardType = KeyboardType.Text
) : TextFieldConfig {
    override val debugLabel: String = "generic_text"

    override fun determineState(input: String): TextFieldState = object : TextFieldState {
        override fun shouldShowError(hasFocus: Boolean) = false

        override fun isValid(): Boolean = input.isNotBlank()

        override fun getErrorMessageResId(): Int? = null

        override fun isFull(): Boolean = false
    }

    override fun filter(userTyped: String): String = userTyped

    override fun convertToRaw(displayName: String) = displayName

    override fun convertFromRaw(rawValue: String) = rawValue
}
