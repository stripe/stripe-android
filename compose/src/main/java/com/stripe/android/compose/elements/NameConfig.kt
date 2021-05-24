package com.stripe.android.compose.elements

import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.compose.R
import com.stripe.android.compose.elements.common.TextFieldConfigInterface
import com.stripe.android.compose.elements.common.TextFieldElementState

internal class NameConfig : TextFieldConfigInterface {
    override val debugLabel = "name"
    override val label = R.string.address_label_name
    override val keyboard = KeyboardType.Text

    override fun determineState(paramFormatted: String?): TextFieldElementState {
        return when {
            paramFormatted?.isEmpty() ?: true -> Error.BlankAndRequired
            else -> Valid.Limitless
        }
    }

    override fun shouldShowError(elementState: TextFieldElementState, hasFocus: Boolean) = false

    override fun filter(userTyped: String) = userTyped.filter { Character.isLetter(it) }

    companion object {
        sealed class Valid : TextFieldElementState.TextFieldElementStateValid() {
            object Limitless : Valid() // no auto-advance
        }

        sealed class Error(stringResId: Int) : TextFieldElementState.TextFieldElementStateError(stringResId) {
            object BlankAndRequired : Error(R.string.blank_and_required)
        }
    }
}