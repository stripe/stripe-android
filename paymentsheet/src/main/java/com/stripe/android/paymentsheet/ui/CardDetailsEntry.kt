package com.stripe.android.paymentsheet.ui

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
        card: PaymentMethod.Card,
        originalCardBrandChoice: CardBrandChoice,
    ): Boolean {
        val expChanged =
            card.expiryMonth != expiryDateState.expiryMonth || card.expiryYear != expiryDateState.expiryYear
        return originalCardBrandChoice != this.cardBrandChoice || expChanged
    }

    fun isComplete(): Boolean {
        if (expiryDateState.enabled.not()) return true
        return expiryDateState.expiryMonth != null && expiryDateState.expiryYear != null
    }
}

/**
 * Converts the CardDetailsEntry to CardUpdateParams.
 *
 * @return CardUpdateParams containing the updated card brand.
 */
internal fun CardDetailsEntry.toUpdateParams(): CardUpdateParams {
    return CardUpdateParams(
        cardBrand = cardBrandChoice.brand,
        expiryMonth = expiryDateState.expiryMonth,
        expiryYear = expiryDateState.expiryYear
    )
}
