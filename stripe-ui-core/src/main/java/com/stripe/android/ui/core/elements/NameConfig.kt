package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.TextFieldStateConstants.Error
import com.stripe.android.ui.core.elements.TextFieldStateConstants.Valid

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class NameConfig : TextFieldConfig {
    @StringRes
    override val label = R.string.address_label_name
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.Words
    override val debugLabel = "name"
    override val keyboard = KeyboardType.Text
    override val visualTransformation: VisualTransformation? = null

    override fun determineState(input: String): TextFieldState {
        return when {
            input.isEmpty() -> Error.Blank
            else -> Valid.Limitless
        }
    }

    override fun filter(userTyped: String) = userTyped.filter { it.isLetter() || it == ' ' }
    override fun convertToRaw(displayName: String) = displayName
    override fun convertFromRaw(rawValue: String) = rawValue
}
