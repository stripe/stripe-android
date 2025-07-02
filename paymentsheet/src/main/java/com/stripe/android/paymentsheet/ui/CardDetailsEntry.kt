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
 * Converts the CardDetailsEntry to CardUpdateParams with billing details.
 *
 * @return CardUpdateParams containing the updated card brand and billing details.
 */
internal fun CardDetailsEntry.toUpdateParams(
    billingDetailsEntry: BillingDetailsEntry?,
): CardUpdateParams {
    return CardUpdateParams(
        cardBrand = cardBrandChoice.brand,
        expiryMonth = expiryDateState.expiryMonth,
        expiryYear = expiryDateState.expiryYear,
        billingDetails = createBillingDetails(billingDetailsEntry)
    )
}

private fun createBillingDetails(
    billingDetailsEntry: BillingDetailsEntry?,
): PaymentMethod.BillingDetails? {
    val address = billingDetailsEntry?.billingDetailsFormState?.let {
        Address(
            city = it.city?.value,
            country = it.country?.value,
            line1 = it.line1?.value,
            line2 = it.line2?.value,
            postalCode = it.postalCode?.value,
            state = it.state?.value
        )
    }

    val email = billingDetailsEntry?.billingDetailsFormState?.email?.value
    val phone = billingDetailsEntry?.billingDetailsFormState?.phone?.value
    val name = billingDetailsEntry?.billingDetailsFormState?.name?.value
    val noContactInfoAvailable = listOf(email, phone, name).all { it.isNullOrBlank() }
    // Only create BillingDetails if we have address or contact information
    if (address == null && noContactInfoAvailable) { return null }

    return PaymentMethod.BillingDetails(
        address = address,
        email = email,
        phone = phone,
        name = name
    )
}
