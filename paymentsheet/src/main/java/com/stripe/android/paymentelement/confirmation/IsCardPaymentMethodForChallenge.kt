package com.stripe.android.paymentelement.confirmation

import com.stripe.android.model.LinkPaymentDetails

/**
 * Determines whether a [PaymentMethodConfirmationOption] represents a card-based payment method.
 *
 * This is used to identify card payments for confirmation challenges that are specific to card
 * payment methods (e.g., attestation, passive challenge).
 */
internal interface IsCardPaymentMethodForChallenge {
    operator fun invoke(confirmationOption: PaymentMethodConfirmationOption): Boolean
}

/**
 * Implementation of [IsCardPaymentMethodForChallenge] for confirmation challenges.
 *
 * Handles both new and saved payment methods, including Link inline card payments where
 * the underlying payment method is a card.
 */
internal object DefaultIsCardPaymentMethodForChallenge : IsCardPaymentMethodForChallenge {
    override fun invoke(confirmationOption: PaymentMethodConfirmationOption): Boolean {
        return when (confirmationOption) {
            is PaymentMethodConfirmationOption.New -> {
                confirmationOption.isCard()
            }
            is PaymentMethodConfirmationOption.Saved -> {
                confirmationOption.isCard()
            }
        }
    }

    /**
     * Checks if a new payment method is card-based.
     *
     * Returns true for:
     * - Direct card payments (typeCode == "card")
     * - Link inline payments where the original payment method was a card
     */
    private fun PaymentMethodConfirmationOption.New.isCard(): Boolean {
        return when (createParams.typeCode) {
            "card" -> true
            "link" -> {
                // Link inline can wrap a card payment method
                createParams.link?.originalPaymentMethodCode == "card"
            }
            else -> false
        }
    }

    /**
     * Checks if a new payment method is card-based.
     *
     * Returns true for:
     * - Direct card payments (typeCode == "card")
     * - Link inline payments where the original payment method was a card
     */
    private fun PaymentMethodConfirmationOption.Saved.isCard(): Boolean {
        val linkCard = paymentMethod.linkPaymentDetails as? LinkPaymentDetails.Card
        return paymentMethod.card != null || linkCard != null || paymentMethod.isLinkPassthroughMode
    }
}
