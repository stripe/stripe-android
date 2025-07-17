package com.stripe.android.link.ui

import androidx.annotation.RestrictTo
import com.stripe.android.model.CardBrand
import com.stripe.android.model.DisplayablePaymentDetails

/**
 * Represents different states of the Link wallet button
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal sealed class LinkButtonState {

    /**
     * Show payment details (card info)
     */
    data class DefaultPayment(
        val cardBrand: CardBrand,
        val last4: String?,
        val numberOfSavedPaymentDetails: Long?
    ) : LinkButtonState()

    /**
     * Show email address
     */
    data class Email(val email: String) : LinkButtonState()

    /**
     * Show signed out state
     */
    object Plain : LinkButtonState()

    companion object Companion {
        /**
         * Create LinkWalletButtonState from email and payment details
         * Priority: payment details -> email -> plain
         */
        fun from(
            email: String?,
            paymentDetails: DisplayablePaymentDetails?
        ): LinkButtonState {
            // Priority 1: Payment details (cards only)
            paymentDetails?.let { details ->
                if (details.defaultPaymentType?.uppercase() == "CARD") {
                    details.defaultCardBrand?.let { brandCode ->
                        return DefaultPayment(
                            cardBrand = CardBrand.fromCode(brandCode),
                            last4 = details.last4,
                            numberOfSavedPaymentDetails = details.numberOfSavedPaymentDetails
                        )
                    }
                }
            }

            // Priority 2: Email
            email?.let {
                return Email(it)
            }

            // Priority 3: Plain (signed out)
            return Plain
        }
    }
}
