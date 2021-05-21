package com.stripe.android.compose.elements

import android.util.Patterns
import androidx.compose.ui.text.input.KeyboardType
import compose.R

class Email : ConfigInterface {
    override val debugLabel = "email"
    override val label = R.string.becs_widget_email
    override val keyboard = KeyboardType.Email

    override fun filter(userTyped: String) =
        userTyped.filter { Character.isLetterOrDigit(it) || it == '.' || it == '@' }

    override fun determineState(displayFormatted: String): ElementState {
        return when {
            displayFormatted.isEmpty() -> Error.BlankAndRequired
            Patterns.EMAIL_ADDRESS.matcher(displayFormatted).matches() -> {
                Valid.Limitless
            }
            else -> {
                Error.Incomplete
            }
        }
    }

    override fun shouldShowError(elementState: ElementState, hasFocus: Boolean) =
        when (elementState) {
            is Error -> {
                when (elementState) {
                    Error.Incomplete -> !hasFocus
                    Error.BlankAndRequired -> false
                }
            }
            is Valid -> false
            else -> false
        }

    companion object {
        sealed class Valid : ElementState.ElementStateValid() {
            object Limitless : Valid() // no auto-advance
        }

        sealed class Error(stringResId: Int) : ElementState.ElementStateError(stringResId) {
            object Incomplete : Error(R.string.incomplete)
            object BlankAndRequired : Error(R.string.blank_and_required)
        }
    }
}