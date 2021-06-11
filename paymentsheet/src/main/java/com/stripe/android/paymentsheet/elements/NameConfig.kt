package com.stripe.android.paymentsheet.elements

import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.common.TextFieldConfig
import com.stripe.android.paymentsheet.elements.common.TextFieldState
import com.stripe.android.paymentsheet.elements.common.TextFieldStateConstants.Error
import com.stripe.android.paymentsheet.elements.common.TextFieldStateConstants.Valid

internal class NameConfig : TextFieldConfig {
    override val debugLabel = "name"
    override val label = R.string.address_label_name
    override val keyboard = KeyboardType.Text

    override fun determineState(input: String): TextFieldState {
        return when {
            input.isEmpty() -> Error.Blank
            else -> Valid.Limitless
        }
    }

    override fun filter(userTyped: String) = userTyped.filter { it.isLetter() }
}