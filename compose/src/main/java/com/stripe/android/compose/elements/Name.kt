package com.stripe.android.compose.elements

import androidx.compose.ui.text.input.KeyboardType
import compose.R

class Name : ConfigInterface {
    override val debugLabel = "name"
    override val label = R.string.address_label_name
    override val keyboard = KeyboardType.Text

    override fun determineState(displayFormatted: String): ElementState {
        return when {
            displayFormatted.isEmpty() -> Error.BlankAndRequired
            else -> Valid.Limitless
        }
    }

    override fun shouldShowError(elementState: ElementState, hasFocus: Boolean) = false

    override fun filter(userTyped: String) = userTyped.filter { Character.isLetter(it) }

    companion object {
        sealed class Valid : ElementState.ElementStateValid() {
            object Limitless : Valid() // no auto-advance
        }

        sealed class Error(stringResId: Int) : ElementState.ElementStateError(stringResId) {
            object BlankAndRequired : Error(R.string.blank_and_required)
        }
    }
}