package com.stripe.android.common.nfcscan.scanner

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.DateUtils
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.R
import javax.inject.Inject

internal interface NfcCardValidator {
    fun validate(cardData: ScannedCardData): Result

    sealed interface Result {
        data object Validated : Result
        data class Invalid(
            val errorCode: String,
            val userMessage: ResolvableString,
        ) : Result
    }
}

internal class DefaultNfcCardValidator @Inject constructor(
    private val paymentMethodMetadata: PaymentMethodMetadata,
) : NfcCardValidator {
    override fun validate(cardData: ScannedCardData): NfcCardValidator.Result {
        if (!paymentMethodMetadata.cardBrandFilter.isAccepted(CardBrand.fromCardNumber(cardData.cardNumber))) {
            return NfcCardValidator.Result.Invalid(
                errorCode = UNSUPPORTED_CARD_VALIDATION_ERROR_CODE,
                userMessage = R.string.stripe_nfc_scan_unsupported_card.resolvableString,
            )
        }

        if (
            !DateUtils.isExpiryDataValid(
                expiryYear = cardData.expirationYear,
                expiryMonth = cardData.expirationMonth,
            )
        ) {
            return NfcCardValidator.Result.Invalid(
                errorCode = EXPIRED_CARD_VALIDATION_ERROR_CODE,
                userMessage = R.string.stripe_nfc_scan_error_expired_card.resolvableString,
            )
        }

        return NfcCardValidator.Result.Validated
    }

    private companion object {
        const val UNSUPPORTED_CARD_VALIDATION_ERROR_CODE = "cardUnsupportedByMerchant"
        const val EXPIRED_CARD_VALIDATION_ERROR_CODE = "expiredCard"
    }
}
