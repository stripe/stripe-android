package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.R
import kotlinx.coroutines.flow.MutableStateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class SimpleTextFieldConfig(
    override val label: ResolvableString,
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.Words,
    override val keyboard: KeyboardType = KeyboardType.Text,
    override val trailingIcon: MutableStateFlow<TextFieldIcon?> = MutableStateFlow(null),
    override val optional: Boolean = false,
    val allowsEmojis: Boolean = true,
) : TextFieldConfig {
    override val debugLabel: String = "generic_text"
    override val visualTransformation: VisualTransformation? = null
    override val loading: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override fun determineState(input: String): TextFieldState = object : TextFieldState {
        override fun shouldShowValidationMessage(
            hasFocus: Boolean,
            isValidating: Boolean
        ) = getValidationMessage() != null && isValidating

        override fun isValid(): Boolean {
            return optional && input.isEmpty() || input.isNotBlank()
        }

        override fun getValidationMessage(): FieldValidationMessage? = if (isValid()) {
            null
        } else {
            FieldValidationMessage.Error(R.string.stripe_blank_and_required)
        }

        override fun isFull(): Boolean = false

        override fun isBlank(): Boolean = input.isBlank()
    }

    override fun filter(userTyped: String): String {
        var filtered = if (
            setOf(KeyboardType.Number, KeyboardType.NumberPassword).contains(keyboard)
        ) {
            userTyped.filter { it.isDigit() }
        } else {
            userTyped
        }

        if (!allowsEmojis) {
            filtered = EMOJI_REGEX.replace(filtered, "")
        }

        return filtered
    }

    override fun convertToRaw(displayName: String) = displayName

    override fun convertFromRaw(rawValue: String) = rawValue
}
