package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.flow.MutableStateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class SimpleTextFieldConfig(
    @StringRes override val label: Int? = null,
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.Words,
    override val keyboard: KeyboardType = KeyboardType.Text,
    override val trailingIcon: MutableStateFlow<TextFieldIcon?> = MutableStateFlow(null)
) : TextFieldConfig {
    override val debugLabel: String = "generic_text"
    override val visualTransformation: VisualTransformation? = null
    override val loading: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override fun determineState(input: String): TextFieldState = object : TextFieldState {
        override fun shouldShowError(hasFocus: Boolean) = false

        override fun isValid(): Boolean = input.isNotBlank()

        override fun getError(): FieldError? = null

        override fun isFull(): Boolean = false

        override fun isBlank(): Boolean = input.isBlank()
    }

    override fun filter(userTyped: String): String =
        if (
            setOf(KeyboardType.Number, KeyboardType.NumberPassword).contains(keyboard)
        ) {
            userTyped.filter { it.isDigit() }
        } else {
            userTyped
        }

    override fun convertToRaw(displayName: String) = displayName

    override fun convertFromRaw(rawValue: String) = rawValue
}
