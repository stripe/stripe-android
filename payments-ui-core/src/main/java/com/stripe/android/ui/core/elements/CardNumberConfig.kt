package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.CardUtils
import com.stripe.android.model.CardBrand
import com.stripe.android.ui.core.R

internal class CardNumberConfig : CardDetailsTextFieldConfig {
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.None
    override val debugLabel: String = "Card number"
    override val label: Int = R.string.acc_label_card_number
    override val keyboard: KeyboardType = KeyboardType.NumberPassword
    override val visualTransformation: VisualTransformation = CardNumberVisualTransformation(' ')

    override fun determineState(brand: CardBrand, number: String, numberAllowedDigits: Int): TextFieldState {
        val luhnValid = CardUtils.isValidLuhnNumber(number)
        val isDigitLimit = brand.getMaxLengthForCardNumber(number) != -1

        return if (number.isBlank()) {
            TextFieldStateConstants.Error.Blank
        } else if (brand == CardBrand.Unknown) {
            TextFieldStateConstants.Error.Invalid(R.string.invalid_card_number)
        } else if (isDigitLimit && number.length < numberAllowedDigits) {
            TextFieldStateConstants.Error.Incomplete(R.string.invalid_card_number)
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
