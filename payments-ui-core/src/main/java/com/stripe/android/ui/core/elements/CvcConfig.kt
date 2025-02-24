package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.model.CardBrand
import com.stripe.android.uicore.elements.TextFieldState
import com.stripe.android.uicore.elements.TextFieldStateConstants
import com.stripe.android.R as StripeR

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CvcConfig : CardDetailsTextFieldConfig {
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.None
    override val debugLabel: String = "cvc"
    override val label: Int = StripeR.string.stripe_cvc_number_hint
    override val keyboard: KeyboardType = KeyboardType.NumberPassword

    override fun determineVisualTransformation(number: String, panLength: Int): VisualTransformation {
        return VisualTransformation.None
    }

    override fun determineState(
        brand: CardBrand,
        number: String,
        numberAllowedDigits: Int
    ): TextFieldState {
        val isDigitLimit = brand.maxCvcLength != -1
        return if (number.isEmpty()) {
            TextFieldStateConstants.Error.Blank
        } else if (brand == CardBrand.Unknown) {
            when (number.length) {
                numberAllowedDigits -> TextFieldStateConstants.Valid.Full
                else -> TextFieldStateConstants.Valid.Limitless
            }
        } else if (isDigitLimit && number.length < numberAllowedDigits) {
            TextFieldStateConstants.Error.Incomplete(StripeR.string.stripe_invalid_cvc)
        } else if (isDigitLimit && number.length > numberAllowedDigits) {
            TextFieldStateConstants.Error.Invalid(StripeR.string.stripe_invalid_cvc)
        } else if (isDigitLimit && number.length == numberAllowedDigits) {
            TextFieldStateConstants.Valid.Full
        } else {
            TextFieldStateConstants.Error.Invalid(StripeR.string.stripe_invalid_cvc)
        }
    }

    override fun filter(userTyped: String): String = userTyped.filter { it.isDigit() }

    override fun convertToRaw(displayName: String): String = displayName

    override fun convertFromRaw(rawValue: String): String = rawValue
}
