package com.stripe.android.paymentsheet.ui

import com.stripe.android.paymentsheet.CardUpdateParams

/**
 * Represents the editable details of a card payment method.
 *
 * @property cardBrandChoice The currently selected card brand choice.
 */
internal data class CardDetailsEntry(
    val cardBrandChoice: CardBrandChoice,
) {
    /**
     * Determines if the card details have changed compared to the provided values.
     *
     * @param originalCardBrandChoice The card brand choice to compare against.
     * @return True if any of the card details have changed, false otherwise.
     */
    fun hasChanged(
        originalCardBrandChoice: CardBrandChoice,
    ): Boolean {
        return originalCardBrandChoice != this.cardBrandChoice
    }
}

/**
 * Converts the CardDetailsEntry to CardUpdateParams.
 *
 * @return CardUpdateParams containing the updated card brand.
 */
internal fun CardDetailsEntry.toUpdateParams(): CardUpdateParams {
    return CardUpdateParams(cardBrand = cardBrandChoice.brand)
}
