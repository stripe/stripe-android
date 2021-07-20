package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.paymentsheet.R
import java.math.BigInteger
import java.util.Locale

/**
 * A text field configuration for an IBAN, or International Bank Account Number, as defined in
 * ISO 13616-1.
 *
 * @see [IBAN on Wikipedia](https://en.wikipedia.org/wiki/International_Bank_Account_Number)
 */
internal class IbanConfig : TextFieldConfig {
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.Characters
    override val debugLabel = "iban"

    @StringRes
    override val label = R.string.stripe_paymentsheet_iban
    override val keyboard = KeyboardType.Ascii
    override val visualTransformation: VisualTransformation = VisualTransformation { text ->
        val output = StringBuilder()
        text.text.forEachIndexed { i, char ->
            output.append(char)
            if (i % 4 == 3 && i < MAX_LENGTH - 1) output.append(" ")
        }
        TransformedText(
            AnnotatedString(output.toString()),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = offset + offset / 4
                override fun transformedToOriginal(offset: Int) = offset - offset / 5
            }
        )
    }

    override fun filter(userTyped: String) =
        userTyped.filter { VALID_INPUT_RANGES.contains(it) }.take(MAX_LENGTH).uppercase()

    override fun convertToRaw(displayName: String) = displayName

    override fun convertFromRaw(rawValue: String) = rawValue

    override fun determineState(input: String): TextFieldState {
        input.ifBlank {
            return TextFieldStateConstants.Error.Blank
        }

        val countryCode = input.take(2).uppercase()
        // First 2 characters represent the country code. Any number means it's invalid
        if (countryCode.any { it.isDigit() }) {
            return TextFieldStateConstants.Error.Invalid(
                R.string.stripe_paymentsheet_iban_invalid_start
            )
        }

        if (countryCode.length < 2) {
            // User might still be entering a valid country code
            return TextFieldStateConstants.Error.Incomplete
        }

        if (!Locale.getISOCountries().contains(countryCode)) {
            return TextFieldStateConstants.Error.Invalid(
                R.string.stripe_paymentsheet_iban_invalid_country
            )
        }

        if (input.length < MIN_LENGTH) {
            return TextFieldStateConstants.Error.Incomplete
        }

        // 1. Move the four initial characters to the end of the string
        // 2. Convert letters to numbers, where A = 10, B = 11, ..., Z = 35
        // 3. Interpret the string as a decimal integer and compute the mod 97
        val isValid = input.takeLast(input.length - 4).plus(input.take(4)).uppercase().replace(
            Regex("[A-Z]")
        ) {
            (it.value.first() - 'A' + 10).toString()
        }.toBigInteger().mod(BigInteger("97")).equals(BigInteger.ONE)

        return if (isValid) {
            if (input.length == MAX_LENGTH) {
                TextFieldStateConstants.Valid.Full
            } else {
                TextFieldStateConstants.Valid.Limitless
            }
        } else {
            TextFieldStateConstants.Error.Invalid(
                R.string.stripe_paymentsheet_iban_invalid
            )
        }
    }

    private companion object {
        const val MIN_LENGTH = 8 // Length varies per country, but is at least 8
        const val MAX_LENGTH = 34
        val VALID_INPUT_RANGES = ('0'..'9') + ('a'..'z') + ('A'..'Z')
    }
}
