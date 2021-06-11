package com.stripe.android.paymentsheet.elements

import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.common.TextFieldConfig
import com.stripe.android.paymentsheet.elements.common.TextFieldState
import com.stripe.android.paymentsheet.elements.common.TextFieldStateConstants.Error
import com.stripe.android.paymentsheet.elements.common.TextFieldStateConstants.Valid
import java.util.regex.Pattern

internal class EmailConfig() :
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
            input.isEmpty() -> Error.Blank
            PATTERN.matcher(input).matches() -> Valid.Limitless
            containsNameAndDomain(input) -> Error.Invalid
            else -> Error.Incomplete
        }
    }

    private fun containsNameAndDomain(str: String) = str.contains("@") && str.matches(
        Regex(".*@.*\\..+")
    )

    companion object {
        // This is copied from Paterns.EMAIL_ADDRESS because it is not defined for unit tests unless
        // using Robolectric which is quite slow.
        val PATTERN: Pattern = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                "\\@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"
        )
    }
}