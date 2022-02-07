package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.TextFieldStateConstants.Error
import com.stripe.android.ui.core.elements.TextFieldStateConstants.Valid
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.TextFieldStateConstants.Error
import com.stripe.android.paymentsheet.elements.TextFieldStateConstants.Valid
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.regex.Pattern

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class EmailConfig : TextFieldConfig {
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.None
    override val debugLabel = "email"

    @StringRes
    override val label = R.string.email
    override val keyboard = KeyboardType.Email
    override val visualTransformation: VisualTransformation? = null
    override val trailingIcon: MutableStateFlow<TextFieldIcon?> = MutableStateFlow(null)

    /**
     * This will allow all characters, but will show as invalid if it doesn't match
     * the regular expression.
     */
    override fun filter(userTyped: String) = userTyped

    override fun convertToRaw(displayName: String) = displayName

    override fun convertFromRaw(rawValue: String) = rawValue

    override fun determineState(input: String): TextFieldState {
        return when {
            input.isEmpty() -> Error.Blank
            PATTERN.matcher(input).matches() -> Valid.Limitless
            containsNameAndDomain(input) -> Error.Invalid(R.string.email_is_invalid)
            else -> Error.Incomplete(R.string.email_is_invalid)
        }
    }

    private fun containsNameAndDomain(str: String) = str.contains("@") && str.matches(
        Regex(".*@.*\\..+")
    )

    companion object {
        // This is copied from Patterns.EMAIL_ADDRESS because it is not defined for unit tests
        // unless using Robolectric which is quite slow.
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
