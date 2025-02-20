package com.stripe.form.fields.card

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.uicore.elements.ExpiryDateVisualTransformation
import com.stripe.form.ContentBox
import com.stripe.form.FormFieldSpec
import com.stripe.form.FormFieldState
import com.stripe.form.ValidationResult
import com.stripe.form.Validator
import com.stripe.form.ValueChange
import com.stripe.form.fields.TextFieldSpec
import com.stripe.form.key
import com.stripe.form.text.TextSpec
import java.util.Calendar

data class ExpiryDateSpec(
    override val state: State
): FormFieldSpec<String> {

    @Composable
    override fun Content(modifier: Modifier) {
        var date by remember {
            mutableStateOf(state.initialValue)
        }
        ContentBox(
            modifier = modifier,
            spec = TextFieldSpec(
                state = TextFieldSpec.TextFieldState(
                    key = key("dateInput"),
                    label = TextSpec("MM/YY"),
                    initialValue = TextFieldValue(state.initialValue),
                    validator = {
                        state.validator(it.text)
                    },
                    onValueChange = { change ->
                        date = change.value.text
                        state.onValueChange(
                            ValueChange(
                                key = state.key,
                                value = change.value.text,
                                isComplete = state.validator(change.value.text).isValid
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number
                    ),
                    maxLength = 4,
                    visualTransformation = state.visualTransformation(date)
                )
            )
        )
    }

    data class State(
        val initialValue: String = "",
        val visualTransformation: (String) -> VisualTransformation = { ExpiryDateVisualTransformation() },
        override val onValueChange: (ValueChange<String>) -> Unit,
        override val validator: (String) -> ValidationResult = { ExpiryValidator.validateResult(it) }
    ): FormFieldState<String> {
        override val key = KEY
    }

    companion object {
        val KEY = key<String>("expiryDate")
    }
}

private object ExpiryValidator: Validator<String> {
    override fun validateResult(value: String): ValidationResult {
        val newString = convertTo4DigitDate(value)
        return when {
            newString.length < 4 -> {
                ValidationResult.Invalid(message = null)
            }
            newString.length > 4 -> {
                ValidationResult.Invalid(message = TextSpec("too long"))
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

    private fun determineTextFieldState(
        month1Based: Int,
        twoDigitYear: Int,
        current1BasedMonth: Int,
        currentYear: Int,
    ): ValidationResult {
        val twoDigitCurrentYear = currentYear % 100

        val isExpiredYear = (twoDigitYear - twoDigitCurrentYear) < 0
        val isYearTooLarge = (twoDigitYear - twoDigitCurrentYear) > 50

        val isExpiredMonth = (twoDigitYear - twoDigitCurrentYear) == 0 && current1BasedMonth > month1Based
        val isMonthInvalid = month1Based !in 1..12

        return if (isExpiredYear) {
            ValidationResult.Invalid(message = TextSpec("invalid year"))
        } else if (isYearTooLarge) {
            ValidationResult.Invalid(message = TextSpec("invalid year"))
        } else if (isExpiredMonth) {
            ValidationResult.Invalid(message = TextSpec("invalid month"))
        } else if (isMonthInvalid) {
            ValidationResult.Invalid(message = TextSpec("invalid month"))
        } else {
            ValidationResult.Valid
        }
    }

}

fun convertTo4DigitDate(input: String) =
    "0$input".takeIf {
        (input.isNotBlank() && !(input[0] == '0' || input[0] == '1')) ||
            ((input.length > 1) && (input[0] == '1' && requireNotNull(input[1].digitToInt()) > 2))
    } ?: input