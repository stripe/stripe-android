package com.stripe.android.paymentsheet.elements

import android.util.Patterns
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.paymentsheet.elements.common.TextFieldConfigInterface
import com.stripe.android.paymentsheet.elements.common.TextFieldElementState
import com.stripe.android.paymentsheet.R
import java.util.regex.Pattern

internal class EmailConfig(private val pattern: Pattern = Patterns.EMAIL_ADDRESS) :
    TextFieldConfigInterface {
    override val debugLabel = "email"
    override val label = R.string.becs_widget_email
    override val keyboard = KeyboardType.Email

    override fun filter(userTyped: String) =
        userTyped.filter { Character.isLetterOrDigit(it) || it == '.' || it == '@' }

    override fun determineState(paramFormatted: String?): TextFieldElementState {
        return when {
            paramFormatted?.isEmpty() ?: true -> Error.BlankAndRequired
            pattern.matcher(paramFormatted).matches() -> {
                Valid.Limitless
            }
            else -> {
                Error.Incomplete
            }
        }
    }

    override fun shouldShowError(elementState: TextFieldElementState, hasFocus: Boolean) =
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
        sealed class Valid : TextFieldElementState.TextFieldElementStateValid() {
            object Limitless : Valid() // no auto-advance
        }

        sealed class Error(stringResId: Int) :
            TextFieldElementState.TextFieldElementStateError(stringResId) {
            object Incomplete : Error(R.string.incomplete)
            object BlankAndRequired : Error(R.string.blank_and_required)
        }
    }
}