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

    override fun determineState(input: String): TextFieldElementState {
        return when {
            input.isEmpty() -> Invalid.BlankAndRequired
            pattern.matcher(input).matches() -> Valid.Limitless
            containsNameAndDomain(input) -> Invalid.Malformed
            else -> Invalid.Incomplete
        }
    }

    private fun containsNameAndDomain(str: String) = str.contains("@") && str.matches(
        Regex(".*@.*\\..+")
    )

    companion object {
        sealed class Valid : TextFieldElementState.TextFieldElementStateValid() {
            object Limitless : Valid() // no auto-advance
            {
                override fun isFull(): Boolean = false
            }
        }

        sealed class Invalid :
            TextFieldElementState.TextFieldElementStateInvalid() {
            object Incomplete : Invalid() {
                override fun shouldShowError(hasFocus: Boolean): Boolean = !hasFocus
                override fun getErrorMessageResId(): Int = R.string.incomplete
            }

            object Malformed : Invalid() {
                override fun shouldShowError(hasFocus: Boolean): Boolean = true
                override fun getErrorMessageResId(): Int = R.string.malformed
            }

            object BlankAndRequired : Invalid() {
                override fun shouldShowError(hasFocus: Boolean): Boolean = false
                override fun getErrorMessageResId(): Int = R.string.blank_and_required
            }
        }
    }
}