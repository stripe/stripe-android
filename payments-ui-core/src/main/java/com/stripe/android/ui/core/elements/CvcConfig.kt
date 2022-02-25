package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.model.CardBrand
import com.stripe.android.ui.core.R

internal class CvcConfig : CardDetailsTextFieldConfig {
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.None
    override val debugLabel: String = "cvc"
    override val label: Int = R.string.cvc_number_hint
    override val keyboard: KeyboardType = KeyboardType.NumberPassword
    override val visualTransformation: VisualTransformation = VisualTransformation.None

    override fun determineState(
        brand: CardBrand,
        number: String,
        numberAllowedDigits: Int
    ): TextFieldState {
        val isDigitLimit = brand.maxCvcLength != -1
        return if (number.isEmpty()) {
            TextFieldStateConstants.Error.Blank
        } else if (brand == CardBrand.Unknown) {
            TextFieldStateConstants.Error.Invalid(R.string.invalid_card_number)
        } else if (isDigitLimit && number.length < numberAllowedDigits) {
            TextFieldStateConstants.Error.Incomplete(R.string.invalid_cvc)
        } else if (isDigitLimit && number.length > numberAllowedDigits) {
            TextFieldStateConstants.Error.Invalid(R.string.invalid_cvc)
        } else if (isDigitLimit && number.length == numberAllowedDigits) {
            TextFieldStateConstants.Valid.Full
        } else {
            TextFieldStateConstants.Error.Invalid(R.string.invalid_cvc)
        }
    }

    override fun filter(userTyped: String): String = userTyped.filter { it.isDigit() }

    override fun convertToRaw(displayName: String): String = displayName

    override fun convertFromRaw(rawValue: String): String = rawValue
}
