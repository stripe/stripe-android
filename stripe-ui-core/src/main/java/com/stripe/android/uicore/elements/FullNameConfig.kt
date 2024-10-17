package com.stripe.android.uicore.elements

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FullNameConfig(
    override val label: Int?
) : TextFieldConfig {
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.Words
    override val debugLabel: String = "full_name"
    override val keyboard: KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text
    override val visualTransformation: VisualTransformation? = null
    override val trailingIcon: StateFlow<TextFieldIcon?> = MutableStateFlow(null)
    override val loading: StateFlow<Boolean> = MutableStateFlow(false)

    override fun determineState(input: String): TextFieldState {
        return if (input.isBlank()) {
            TextFieldStateConstants.Error.Blank
        } else {
            TextFieldStateConstants.Valid.Limitless
        }
    }

    override fun filter(userTyped: String): String {
        return userTyped
    }

    override fun convertToRaw(displayName: String): String {
        return displayName
    }

    override fun convertFromRaw(rawValue: String): String {
        return rawValue
    }
}