package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.ui.core.R

internal class CardNumberConfig : CardDetailsTextFieldConfig {
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.None
    override val debugLabel: String = "Card number"
    override val label: Int = R.string.acc_label_card_number
    override val keyboard: KeyboardType = KeyboardType.NumberPassword
    override val visualTransformation: VisualTransformation = CardNumberVisualTransformation(' ')

    override fun determineState(brand: CardBrand, number: String): TextFieldState {
        val luhnValid = CardUtils.isValidLuhnNumber(number)
        val isDigitLimit = brand.getMaxLengthForCardNumber(number) != -1
        // This only accounts for the hard coded card brand information not the card metadata
        // service
        val numberAllowedDigits = brand.getMaxLengthForCardNumber(number)

        return if (number.isBlank()) {
            TextFieldStateConstants.Error.Blank
        } else if (brand == CardBrand.Unknown) {
            TextFieldStateConstants.Error.Invalid(R.string.invalid_card_number)
        } else if (isDigitLimit && number.length < numberAllowedDigits) {
            TextFieldStateConstants.Error.Incomplete(R.string.invalid_card_number)
        } else if (isDigitLimit && number.length > numberAllowedDigits) {
            object : TextFieldState {
                override fun shouldShowError(hasFocus: Boolean) = true

                // We will assume we don't know the correct number of numbers until we get
                // the metadata service added back in
                override fun isValid() = true
                override fun isFull() = true
                override fun isBlank() = false
                override fun getError() = FieldError(
                    R.string.card_number_longer_than_expected
                )
            }
        } else if (!luhnValid) {
            TextFieldStateConstants.Error.Invalid(R.string.invalid_card_number)
        } else if (isDigitLimit && number.length == numberAllowedDigits) {
            TextFieldStateConstants.Valid.Full
        } else {
            TextFieldStateConstants.Error.Invalid(R.string.invalid_card_number)
        }
    }

    override fun filter(userTyped: String): String = userTyped.filter { it.isDigit() }

    override fun convertToRaw(displayName: String): String = displayName

    override fun convertFromRaw(rawValue: String): String = rawValue
}
