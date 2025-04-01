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
    val expMonth: Int? = null,
    val expYear: Int? = null,
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
        val expChanged = card.expiryMonth != expMonth || card.expiryYear != expYear
        return originalCardBrandChoice != this.cardBrandChoice || expChanged
    }

    fun isComplete(expiryDateEditable: Boolean): Boolean {
        if (expiryDateEditable.not()) return true
        return expMonth != null && expYear != null
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
        expiryMonth = expMonth,
        expiryYear = expYear
    )
}
