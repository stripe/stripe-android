package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.TextFieldStateConstants.Error
import com.stripe.android.paymentsheet.elements.TextFieldStateConstants.Valid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Calendar

internal class DateConfig : TextFieldConfig {
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.None
    override val debugLabel = "date"

    @StringRes
    override val label = R.string.credit_expiration_date
    override val keyboard = KeyboardType.NumberPassword
    override val visualTransformation = ExpiryDateVisualTransformation()
    override val trailingIcon: Flow<TextFieldIcon?> = MutableStateFlow(null)

    /**
     * This will allow all characters, but will show as invalid if it doesn't match
     * the regular expression.
     */
    override fun filter(userTyped: String) = userTyped.filter { it.isDigit() }

    override fun convertToRaw(displayName: String) = displayName

    override fun convertFromRaw(rawValue: String) = rawValue

    override fun determineState(input: String): TextFieldState {
        return if (input.isBlank()) {
            Error.Blank
        } else {
            val newString =
                if ((
                        input.isNotBlank() &&
                            !(input[0] == '0' || input[0] == '1')
                        ) ||
                    (
                        (input.length > 1) &&
                            (input[0] == '1' && requireNotNull(input[1].digitToInt()) > 2)
                        )
                ) {
                    "0$input"
                } else {
                    input
                }
            when {
                newString.length < 4 -> {
                    Error.Incomplete(R.string.incomplete_expiry_date)
                }
                newString.length > 4 -> {
                    Error.Invalid(R.string.invalid_expiry_date)
                }
                else -> {
                    val month = requireNotNull(newString.take(2).toIntOrNull())
                    val year = requireNotNull(newString.takeLast(2).toIntOrNull())
                    val yearMinus1900 = year + (2000 - 1900)
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR) - 1900
                    val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
                    if ((yearMinus1900 - currentYear) < 0) {
                        Error.Invalid(R.string.invalid_expiry_year_past)
                    } else if ((yearMinus1900 - currentYear) > 50) {
                        Error.Invalid(R.string.invalid_expiry_year)
                    } else if ((yearMinus1900 - currentYear) == 0 && currentMonth > month) {
                        Error.Invalid(R.string.invalid_expiry_year_past)
                    } else if (month !in 1..12) {
                        Error.Incomplete(R.string.invalid_expiry_month)
                    } else {
                        Valid.Full
                    }
                }
            }
        }
    }

}
