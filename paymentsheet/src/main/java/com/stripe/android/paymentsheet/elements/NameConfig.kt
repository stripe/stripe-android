package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.TextFieldStateConstants.Error
import com.stripe.android.paymentsheet.elements.TextFieldStateConstants.Valid

internal class NameConfig : TextFieldConfig {
    @StringRes
    override val label = R.string.address_label_name
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.Words
    override val debugLabel = "name"
    override val keyboard = KeyboardType.Text

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
