package com.stripe.android.paymentsheet.elements

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.paymentsheet.R

internal class CardNumberConfig : CreditTextFieldConfig {
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.None
    override val debugLabel: String = "Card number"
    override val label: Int = R.string.card_number_label
    override val keyboard: KeyboardType = KeyboardType.Number
    override val visualTransformation: VisualTransformation = CardNumberVisualTransformation(' ')

    override fun determineState(brand: CardBrand, number: String): TextFieldState {
        val luhnValid = isValidLuhnNumber(number)
        val isDigitLimit = brand.getMaxLengthForCardNumber(number) != -1
        val numberAllowedDigits = brand.getMaxLengthForCardNumber(number) // Accounts for variant max length

        return if (number.isBlank()) {
            TextFieldStateConstants.Error.Blank
        } else if (brand == CardBrand.Unknown) {
            TextFieldStateConstants.Error.Invalid(R.string.card_number_invalid_brand)
        } else if (isDigitLimit && number.length < numberAllowedDigits) {
            TextFieldStateConstants.Error.Incomplete(R.string.card_number_incomplete)
        } else if (isDigitLimit && number.length > numberAllowedDigits) {
            object : TextFieldState {
                override fun shouldShowError(hasFocus: Boolean) = true
                override fun isValid() = true
                override fun isFull() = true
                override fun isBlank() = false
                override fun getError() = FieldError(
                    R.string.card_number_longer_than_expected
                )
            }
        } else if (!luhnValid) {
            TextFieldStateConstants.Error.Invalid(R.string.card_number_invalid_luhn)
        } else if (isDigitLimit && number.length == numberAllowedDigits) {
            TextFieldStateConstants.Valid.Full
        } else {
            TextFieldStateConstants.Error.Invalid(R.string.card_number_invalid) // TODO: Double check this case
        }
    }

    /**
     * COPIED FROM CARDUTILS.kt
     * Checks the input string to see whether or not it is a valid Luhn number.
     *
     * @param cardNumber a String that may or may not represent a valid Luhn number
     * @return `true` if and only if the input value is a valid Luhn number
     */
    private fun isValidLuhnNumber(cardNumber: String?): Boolean {
        if (cardNumber == null) {
            return false
        }

        var isOdd = true
        var sum = 0

        for (index in cardNumber.length - 1 downTo 0) {
            val c = cardNumber[index]
            if (!c.isDigit()) {
                return false
            }

            var digitInteger = Character.getNumericValue(c)
            isOdd = !isOdd

            if (isOdd) {
                digitInteger *= 2
            }

            if (digitInteger > 9) {
                digitInteger -= 9
            }

            sum += digitInteger
        }

        return sum % 10 == 0
    }

    override fun filter(userTyped: String): String = userTyped.filter { it.isDigit() }

    override fun convertToRaw(displayName: String): String = displayName

    override fun convertFromRaw(rawValue: String): String = rawValue
}
