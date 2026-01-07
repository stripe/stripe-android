package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.CardUtils
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardFunding
import com.stripe.android.uicore.elements.FieldValidationMessage
import com.stripe.android.uicore.elements.TextFieldState
import com.stripe.android.uicore.elements.TextFieldStateConstants
import com.stripe.android.R as StripeR

internal class CardNumberConfig(
    private val isCardBrandChoiceEligible: Boolean,
    private val cardBrandFilter: CardBrandFilter,
    private val cardFundingFilter: CardFundingFilter
) : CardNumberTextFieldConfig {
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.None
    override val debugLabel: String = "Card number"
    override val label: ResolvableString = resolvableString(StripeR.string.stripe_acc_label_card_number)
    override val keyboard: KeyboardType = KeyboardType.NumberPassword

    // Hardcoded number of card digits + a buffer entered before we hit the card metadata service in CBC
    private var digitsRequiredToFetchBrands = 9

    // Minimum number of digits required to fetch funding type from BIN lookup
    private val digitsRequiredToFetchFunding = 6

    override fun determineVisualTransformation(number: String, panLength: Int): VisualTransformation {
        return when (panLength) {
            14, 15 -> CardNumberVisualTransformations.FourteenAndFifteenPanLength(SEPARATOR)
            19 -> CardNumberVisualTransformations.NineteenPanLength(SEPARATOR)
            else -> CardNumberVisualTransformations.Default(SEPARATOR)
        }
    }

    override fun determineState(
        brand: CardBrand,
        funding: CardFunding?,
        number: String,
        numberAllowedDigits: Int,
    ): TextFieldState {
        if (number.isBlank()) {
            return TextFieldStateConstants.Error.Blank
        }

        val brandError = checkBrandError(brand, number)
        if (brandError != null) {
            return brandError
        }

        if (brand == CardBrand.Unknown) {
            return TextFieldStateConstants.Error.Invalid(
                errorMessageResId = StripeR.string.stripe_invalid_card_number,
                preventMoreInput = true,
            )
        }

        val isDigitLimit = brand.getMaxLengthForCardNumber(number) != -1
        val fundingErrorMessageId = cardFundingFilter.allowedFundingTypesDisplayMessage()
        val hasFundingError = checkFundingError(funding, number, fundingErrorMessageId)

        return when {
            isDigitLimit && number.length < numberAllowedDigits -> {
                handleIncompleteNumber(hasFundingError, fundingErrorMessageId)
            }
            !CardUtils.isValidLuhnNumber(number) -> {
                TextFieldStateConstants.Error.Invalid(
                    errorMessageResId = StripeR.string.stripe_invalid_card_number,
                    preventMoreInput = true,
                )
            }
            isDigitLimit && number.length == numberAllowedDigits -> {
                handleCompleteNumber(hasFundingError, fundingErrorMessageId)
            }
            else -> {
                TextFieldStateConstants.Error.Invalid(StripeR.string.stripe_invalid_card_number)
            }
        }
    }

    private fun checkBrandError(brand: CardBrand, number: String): TextFieldState? {
        val shouldShowBrandError = !cardBrandFilter.isAccepted(brand) &&
            (!isCardBrandChoiceEligible || number.length > digitsRequiredToFetchBrands)

        return if (shouldShowBrandError) {
            /*
              If the merchant is eligible for CBC do not show the disallowed error
              until we have had time to hit the card metadata service for a list of possible brands
             */
            TextFieldStateConstants.Error.Invalid(
                errorMessageResId = StripeR.string.stripe_disallowed_card_brand,
                formatArgs = listOf(brand.displayName),
                preventMoreInput = false,
            )
        } else {
            null
        }
    }

    private fun checkFundingError(
        funding: CardFunding?,
        number: String,
        fundingErrorMessageId: Int?
    ): Boolean {
        val cardFundingAccepted = funding?.let(cardFundingFilter::isAccepted)
        return number.length >= digitsRequiredToFetchFunding &&
            cardFundingAccepted == false &&
            fundingErrorMessageId != null
    }

    private fun handleIncompleteNumber(
        hasFundingError: Boolean,
        fundingErrorMessageId: Int?
    ): TextFieldState {
        return if (hasFundingError && fundingErrorMessageId != null) {
            TextFieldStateConstants.Error.Invalid(
                validationMessage = FieldValidationMessage.Warning(
                    message = fundingErrorMessageId,
                ),
                preventMoreInput = false,
            )
        } else {
            TextFieldStateConstants.Error.Incomplete(StripeR.string.stripe_invalid_card_number)
        }
    }

    private fun handleCompleteNumber(
        hasFundingError: Boolean,
        fundingErrorMessageId: Int?
    ): TextFieldState {
        return if (hasFundingError && fundingErrorMessageId != null) {
            TextFieldStateConstants.Valid.Full(
                validationMessage = FieldValidationMessage.Warning(fundingErrorMessageId),
            )
        } else {
            TextFieldStateConstants.Valid.Full()
        }
    }

    override fun filter(userTyped: String): String = userTyped.filter { it.isDigit() }

    override fun convertToRaw(displayName: String): String = displayName

    override fun convertFromRaw(rawValue: String): String = rawValue

    private companion object {
        const val SEPARATOR = ' '
    }
}
