package com.stripe.android.paymentsheet.elements

import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.common.TextFieldConfig
import com.stripe.android.paymentsheet.elements.common.TextFieldElementState

internal class NameConfig : TextFieldConfig {
    override val debugLabel = "name"
    override val label = R.string.address_label_name
    override val keyboard = KeyboardType.Text

    override fun determineState(input: String): TextFieldElementState {
        return when {
            input.isEmpty() -> Error.BlankAndRequired
            else -> Valid.Limitless
        }
    }

    override fun shouldShowError(elementState: TextFieldElementState, hasFocus: Boolean) = false

    override fun filter(userTyped: String) = userTyped.filter { it.isLetter() }

    companion object {
        sealed class Valid : TextFieldElementState.TextFieldElementStateValid() {
            object Limitless : Valid() // no auto-advance
        }

        sealed class Error(stringResId: Int) :
            TextFieldElementState.TextFieldElementStateError(stringResId) {
            object BlankAndRequired : Error(R.string.blank_and_required)
        }
    }
}