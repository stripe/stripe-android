package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.TextFieldConfig
import com.stripe.android.uicore.elements.TextFieldIcon
import com.stripe.android.uicore.elements.TextFieldState
import com.stripe.android.uicore.elements.TextFieldStateConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigInteger
import java.util.Locale
import com.stripe.android.R as StripeR

/**
 * A text field configuration for an IBAN, or International Bank Account Number, as defined in
 * ISO 13616-1.
 *
 * @see [IBAN on Wikipedia](https://en.wikipedia.org/wiki/International_Bank_Account_Number)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class IbanConfig : TextFieldConfig {
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.Characters
    override val debugLabel = "iban"

    @StringRes
    override val label = R.string.stripe_iban
    override val keyboard = KeyboardType.Ascii

    override val trailingIcon: MutableStateFlow<TextFieldIcon?> = MutableStateFlow(
        TextFieldIcon.Trailing(
            StripeR.drawable.stripe_ic_bank_generic,
            isTintable = true
        )
    )
    override val loading: StateFlow<Boolean> = MutableStateFlow(false)

    // Displays the IBAN in groups of 4 characters with spaces added between them
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
                R.string.stripe_iban_invalid_start
            )
        }

        if (countryCode.length < 2) {
            // User might still be entering a valid country code
            return TextFieldStateConstants.Error.Incomplete(
                R.string.stripe_iban_incomplete
            )
        }

        if (!Locale.getISOCountries().contains(countryCode)) {
            return TextFieldStateConstants.Error.Invalid(
                R.string.stripe_iban_invalid_country,
                arrayOf(countryCode)
            )
        }

        if (input.length < MIN_LENGTH) {
            return TextFieldStateConstants.Error.Incomplete(
                R.string.stripe_iban_incomplete
            )
        }

        return if (isIbanValid(input)) {
            if (input.length == MAX_LENGTH) {
                TextFieldStateConstants.Valid.Full
            } else {
                TextFieldStateConstants.Valid.Limitless
            }
        } else {
            TextFieldStateConstants.Error.Incomplete(
                StripeR.string.stripe_invalid_bank_account_iban
            )
        }
    }

    /**
     * Verify an IBAN based on the validation algorithm:
     * https://en.wikipedia.org/wiki/International_Bank_Account_Number#Validating_the_IBAN
     *
     * 1. Move the four initial characters to the end of the string
     * 2. Convert letters to numbers, where A = 10, B = 11, ..., Z = 35
     * 3. Interpret the string as a decimal integer and check that the mod 97 is 1
     */
    private fun isIbanValid(iban: String) =
        iban.takeLast(iban.length - 4).plus(iban.take(4)).uppercase()
            .replace(
                Regex("[A-Z]")
            ) {
                (it.value.first() - 'A' + 10).toString()
            }.toBigInteger().mod(BigInteger("97")).equals(BigInteger.ONE)

    private companion object {
        const val MIN_LENGTH = 8 // Length varies per country, but is at least 8
        const val MAX_LENGTH = 34
        val VALID_INPUT_RANGES = ('0'..'9') + ('a'..'z') + ('A'..'Z')
    }
}
