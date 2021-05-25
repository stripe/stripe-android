package com.stripe.android.paymentsheet.elements

import android.util.Patterns
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.common.TextFieldConfig
import com.stripe.android.paymentsheet.elements.common.TextFieldElementState
import java.util.regex.Pattern

internal class EmailConfig(private val pattern: Pattern = Patterns.EMAIL_ADDRESS) :
    TextFieldConfig {
    override val debugLabel = "email"
    override val label = R.string.becs_widget_email
    override val keyboard = KeyboardType.Email

    /**
     * This will allow all characters, but will show as invalid if it doesn't match
     * the regular expression.
     */
    override fun filter(userTyped: String) = userTyped

    override fun determineState(paramFormatted: String): TextFieldElementState {
        return when {
            paramFormatted.isEmpty() -> Error.BlankAndRequired
            pattern.matcher(paramFormatted).matches() -> Valid.Limitless
            else -> Error.Incomplete
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