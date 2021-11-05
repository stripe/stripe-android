package com.stripe.android.viewmodel.credit.cvc

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.CardDetailsTextFieldConfig
import com.stripe.android.paymentsheet.elements.TextFieldState
import com.stripe.android.paymentsheet.elements.TextFieldStateConstants

@Suppress("DEPRECATION")
internal class CvcConfig : CardDetailsTextFieldConfig {
    // TODO: Neecd to support CVV, does this need to come from metadata service?
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.None
    override val debugLabel: String = "cvc"
    override val label: Int = R.string.credit_cvc_label
    override val keyboard: KeyboardType = KeyboardType.NumberPassword
    override val visualTransformation: VisualTransformation = VisualTransformation.None

    override fun determineState(
        brand: CardBrand,
        number: String
    ): TextFieldState {
        val numberAllowedDigits = brand.maxCvcLength
        val isDigitLimit = brand.maxCvcLength != -1
        return if (brand == CardBrand.Unknown) {
            TextFieldStateConstants.Error.Invalid(R.string.card_number_invalid_brand)
        } else if (number.isEmpty()) {
            TextFieldStateConstants.Error.Blank
        } else if (isDigitLimit && number.length < numberAllowedDigits) {
            TextFieldStateConstants.Error.Incomplete(R.string.credit_cvc_incomplete)
        } else if (isDigitLimit && number.length > numberAllowedDigits) {
            TextFieldStateConstants.Error.Invalid(R.string.credit_cvc_too_long)
        } else if (isDigitLimit && number.length == numberAllowedDigits) {
            TextFieldStateConstants.Valid.Full
        } else {
            // TODO: Double check this case
            TextFieldStateConstants.Error.Invalid(R.string.credit_cvc_invalid)
        }
    }

    override fun filter(userTyped: String): String = userTyped.filter { it.isDigit() }

    override fun convertToRaw(displayName: String): String = displayName

    override fun convertFromRaw(rawValue: String): String = rawValue
}
