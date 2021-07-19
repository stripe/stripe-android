package com.stripe.android.paymentsheet.elements

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.paymentsheet.ElementType

internal class GenericTextFieldConfig(
    override val debugLabel: String = "generic_text",
    override val label: Int,
    override val keyboard: KeyboardType = KeyboardType.Text,
    override val elementType: ElementType = ElementType.GenericText,
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.None
) : TextFieldConfig {
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
