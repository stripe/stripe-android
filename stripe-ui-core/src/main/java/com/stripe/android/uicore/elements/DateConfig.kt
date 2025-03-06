package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import com.stripe.android.uicore.R
import com.stripe.android.uicore.elements.TextFieldStateConstants.Error
import com.stripe.android.uicore.elements.TextFieldStateConstants.Valid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DateConfig : TextFieldConfig {
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.None
    override val debugLabel = "date"

    @StringRes
    override val label = R.string.stripe_expiration_date_hint
    override val keyboard = KeyboardType.NumberPassword
    override val visualTransformation = ExpiryDateVisualTransformation()
    override val trailingIcon: StateFlow<TextFieldIcon?> = MutableStateFlow(null)
    override val loading: StateFlow<Boolean> = MutableStateFlow(false)
    override val layoutDirection: LayoutDirection = LayoutDirection.Ltr
    override val shouldAnnounceFieldValue = false
    override val shouldAnnounceLabel = false

    override fun filter(userTyped: String) = userTyped.filter { it.isDigit() }

    override fun convertToRaw(displayName: String) = displayName

    override fun convertFromRaw(rawValue: String) = rawValue

    override fun determineState(input: String): TextFieldState {
        return if (input.isBlank()) {
            Error.Blank
        } else {
            val newString = convertTo4DigitDate(input)
            when {
                newString.length < 4 -> {
                    Error.Incomplete(R.string.stripe_incomplete_expiry_date)
                }
                newString.length > 4 -> {
                    Error.Invalid(R.string.stripe_incomplete_expiry_date)
                }
                else -> {
                    determineTextFieldState(
                        month1Based = requireNotNull(newString.take(2).toIntOrNull()),
                        twoDigitYear = requireNotNull(newString.takeLast(2).toIntOrNull()),
                        // Calendar.getInstance().get(Calendar.MONTH) is 0-based, so add 1
                        current1BasedMonth = Calendar.getInstance().get(Calendar.MONTH) + 1,
                        currentYear = Calendar.getInstance().get(Calendar.YEAR),
                    )
                }
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        @VisibleForTesting
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun determineTextFieldState(
            month1Based: Int,
            twoDigitYear: Int,
            current1BasedMonth: Int,
            currentYear: Int,
        ): TextFieldState {
            val twoDigitCurrentYear = currentYear % 100

            val isExpiredYear = (twoDigitYear - twoDigitCurrentYear) < 0
            val isYearTooLarge = (twoDigitYear - twoDigitCurrentYear) > 50

            val isExpiredMonth = (twoDigitYear - twoDigitCurrentYear) == 0 && current1BasedMonth > month1Based
            val isMonthInvalid = month1Based !in 1..12

            return if (isExpiredYear) {
                Error.Invalid(R.string.stripe_invalid_expiry_year, preventMoreInput = true)
            } else if (isYearTooLarge) {
                Error.Invalid(R.string.stripe_invalid_expiry_year, preventMoreInput = true)
            } else if (isExpiredMonth) {
                Error.Invalid(R.string.stripe_invalid_expiry_month, preventMoreInput = true)
            } else if (isMonthInvalid) {
                Error.Incomplete(R.string.stripe_invalid_expiry_month)
            } else {
                Valid.Full
            }
        }
    }
}
