package com.stripe.android.paymentsheet.ui

import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.CardUpdateParams

/**
 * Represents the editable details of a card payment method.
 *
 * @property cardBrandChoice The currently selected card brand choice.
 */
internal data class CardDetailsEntry(
    val cardBrandChoice: CardBrandChoice,
    val expiryDateState: ExpiryDateState,
) {
    /**
     * Determines if the card details have changed compared to the provided values.
     *
     * @param originalCardBrandChoice The card brand choice to compare against.
     * @return True if any of the card details have changed, false otherwise.
     */
    fun hasChanged(
        editCardPayload: EditCardPayload,
        originalCardBrandChoice: CardBrandChoice,
    ): Boolean {
        return originalCardBrandChoice != this.cardBrandChoice ||
            expiryDateHasChanged(editCardPayload)
    }

    fun isComplete(): Boolean {
        if (expiryDateState.enabled.not()) return true
        return expiryDateState.expiryMonth != null && expiryDateState.expiryYear != null
    }

    private fun expiryDateHasChanged(editCardPayload: EditCardPayload): Boolean {
        return editCardPayload.expiryMonth != expiryDateState.expiryMonth ||
            editCardPayload.expiryYear != expiryDateState.expiryYear
    }
}

/**
 * Converts the CardDetailsEntry to CardUpdateParams.
 *
 * @return CardUpdateParams containing the updated card brand.
 */
internal fun CardDetailsEntry.toUpdateParams(
    billingDetailsEntry: BillingDetailsEntry?
): CardUpdateParams {
    return CardUpdateParams(
        cardBrand = cardBrandChoice.brand,
        expiryMonth = expiryDateState.expiryMonth,
        expiryYear = expiryDateState.expiryYear,
        billingDetails = billingDetailsEntry?.billingDetailsFormState?.let {
            val address = Address(
                city = it.city?.value,
                country = it.country?.value,
                line1 = it.line1?.value,
                line2 = it.line2?.value,
                postalCode = it.postalCode?.value,
                state = it.state?.value
            )
            PaymentMethod.BillingDetails(address)
        }
    )
}
