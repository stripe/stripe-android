package com.stripe.android.paymentsheet.ui

import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.CardUpdateParams
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode

/**
 * Represents the editable details of a card payment method.
 *
 * @property cardBrandChoice The currently selected card brand choice.
 */
internal data class CardDetailsEntry(
    val cardBrandChoice: CardBrandChoice,
    val expiryDateState: ExpiryDateState,
    val billingDetailsEntry: BillingDetailsEntry? = null,
    private val billingDetails: PaymentMethod.BillingDetails?,
    private val addressCollectionMode: AddressCollectionMode,
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
        return originalCardBrandChoice != this.cardBrandChoice ||
            expiryDateHasChanged(card) || billingAddressHasChanged()
    }

    fun isComplete(): Boolean {
        return expiryDateComplete() && billingAddressComplete()
    }

    private fun expiryDateHasChanged(card: PaymentMethod.Card): Boolean {
        return card.expiryMonth != expiryDateState.expiryMonth || card.expiryYear != expiryDateState.expiryYear
    }

    private fun billingAddressHasChanged(): Boolean {
        return billingDetailsEntry?.hasChanged(
            billingDetails = billingDetails,
            addressCollectionMode = addressCollectionMode
        ) ?: false
    }

    private fun expiryDateComplete(): Boolean {
        if (expiryDateState.enabled.not()) return true
        return expiryDateState.expiryMonth != null && expiryDateState.expiryYear != null
    }

    private fun billingAddressComplete(): Boolean {
        return billingDetailsEntry?.isComplete(addressCollectionMode) ?: true
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
