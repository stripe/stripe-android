package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.uicore.elements.TextFieldStateConstants.Error
import com.stripe.android.uicore.elements.TextFieldStateConstants.Valid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.stripe.android.core.R as CoreR

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class NameConfig : TextFieldConfig {
    @StringRes
    override val label = CoreR.string.stripe_address_label_full_name
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.Words
    override val debugLabel = "name"
    override val keyboard = KeyboardType.Text
    override val visualTransformation: VisualTransformation? = null
    override val trailingIcon: MutableStateFlow<TextFieldIcon?> = MutableStateFlow(null)
    override val loading: StateFlow<Boolean> = MutableStateFlow(false)

    override fun determineState(input: String): TextFieldState {
        return when {
            input.isBlank() -> Error.Blank
            else -> Valid.Limitless
        }
    }

    override fun filter(userTyped: String) = userTyped.filter { it.isLetter() || it == ' ' }
    override fun convertToRaw(displayName: String) = displayName
    override fun convertFromRaw(rawValue: String) = rawValue

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun createController(initialValue: String?) = SimpleTextFieldController(
            NameConfig(),
            initialValue = initialValue
        )
    }
}
