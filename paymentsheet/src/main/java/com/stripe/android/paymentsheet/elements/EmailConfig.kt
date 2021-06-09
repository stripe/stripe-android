package com.stripe.android.paymentsheet.elements

import android.util.Patterns
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.common.TextFieldConfig
import com.stripe.android.paymentsheet.elements.common.TextFieldStateConstants.Invalid
import com.stripe.android.paymentsheet.elements.common.TextFieldStateConstants.Valid
import com.stripe.android.paymentsheet.elements.common.TextFieldState
import java.util.regex.Pattern

internal class EmailConfig(private val pattern: Pattern = Patterns.EMAIL_ADDRESS) :
    TextFieldConfig {
    override val debugLabel = "email"
    override val label = R.string.email
    override val keyboard = KeyboardType.Email

    /**
     * This will allow all characters, but will show as invalid if it doesn't match
     * the regular expression.
     */
    override fun filter(userTyped: String) = userTyped

    override fun determineState(input: String): TextFieldState {
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
}