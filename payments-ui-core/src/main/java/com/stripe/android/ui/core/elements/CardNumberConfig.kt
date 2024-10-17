package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.CardBrandFilter
import com.stripe.android.CardUtils
import com.stripe.android.model.CardBrand
import com.stripe.android.uicore.elements.TextFieldState
import com.stripe.android.uicore.elements.TextFieldStateConstants
import com.stripe.android.R as StripeR

internal class CardNumberConfig(
    private val isCBCEligible: Boolean,
    private val cardBrandFilter: CardBrandFilter
)    : CardDetailsTextFieldConfig {
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.None
    override val debugLabel: String = "Card number"
    override val label: Int = StripeR.string.stripe_acc_label_card_number
    override val keyboard: KeyboardType = KeyboardType.NumberPassword
    override val visualTransformation: VisualTransformation = CardNumberVisualTransformation(' ')

    override fun determineState(brand: CardBrand, number: String, numberAllowedDigits: Int): TextFieldState {
        val luhnValid = CardUtils.isValidLuhnNumber(number)
        val isDigitLimit = brand.getMaxLengthForCardNumber(number) != -1

        return if (number.isBlank()) {
            TextFieldStateConstants.Error.Blank
        } else if (!cardBrandFilter.isAccepted(brand) && (!isCBCEligible || number.length > 8)) {
            return TextFieldStateConstants.Error.Invalid(
                errorMessageResId = StripeR.string.stripe_disallowed_card_brand,
                formatArgs = arrayOf(brand.displayName),
                preventMoreInput = false,
            )
        } else if (brand == CardBrand.Unknown) {
            TextFieldStateConstants.Error.Invalid(
                errorMessageResId = StripeR.string.stripe_invalid_card_number,
                preventMoreInput = true,
            )
        } else if (isDigitLimit && number.length < numberAllowedDigits) {
            TextFieldStateConstants.Error.Incomplete(StripeR.string.stripe_invalid_card_number)
        } else if (!luhnValid) {
            TextFieldStateConstants.Error.Invalid(
                errorMessageResId = StripeR.string.stripe_invalid_card_number,
                preventMoreInput = true,
            )
        } else if (isDigitLimit && number.length == numberAllowedDigits) {
            TextFieldStateConstants.Valid.Full
        } else {
            TextFieldStateConstants.Error.Invalid(StripeR.string.stripe_invalid_card_number)
        }
    }

    override fun filter(userTyped: String): String = userTyped.filter { it.isDigit() }

    override fun convertToRaw(displayName: String): String = displayName

    override fun convertFromRaw(rawValue: String): String = rawValue
}
