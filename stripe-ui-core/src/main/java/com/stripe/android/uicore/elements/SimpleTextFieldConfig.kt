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
    private val textFilter: TextFilter? = null,
) : TextFieldConfig {
    override val debugLabel: String = "generic_text"
    override val visualTransformation: VisualTransformation? = null
    override val loading: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override fun determineState(input: String): TextFieldState = object : TextFieldState {
        override fun shouldShowError(hasFocus: Boolean, isValidating: Boolean) = getError() != null && isValidating

        override fun isValid(): Boolean {
            return optional && input.isEmpty() || input.isNotBlank()
        }

        override fun getError(): FieldError? = if (isValid()) {
            null
        } else {
            FieldError(R.string.stripe_blank_and_required)
        }

        override fun isFull(): Boolean = false

        override fun isBlank(): Boolean = input.isBlank()
    }

    override fun filter(userTyped: String): String {
        val preFiltered = when {
            setOf(KeyboardType.Number, KeyboardType.NumberPassword).contains(keyboard) -> {
                userTyped.filter { it.isDigit() }
            }
            else -> userTyped
        }
        return textFilter?.filter(preFiltered) ?: preFiltered
    }

    override fun convertToRaw(displayName: String) = displayName

    override fun convertFromRaw(rawValue: String) = rawValue
}
