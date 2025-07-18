package com.stripe.android.link.ui

import androidx.annotation.RestrictTo
import com.stripe.android.link.ui.wallet.DefaultPaymentUI
import com.stripe.android.link.ui.wallet.toDefaultPaymentUI
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
        val paymentUI: DefaultPaymentUI
    ) : LinkButtonState()

    /**
     * Show email address
     */
    data class Email(val email: String) : LinkButtonState()

    /**
     * Show signed out state
     */
    object Default : LinkButtonState()

    companion object {
        /**
         * Creates appropriate LinkButtonState based on configuration and payment details.
         */
        fun create(
            linkEmail: String?,
            paymentDetails: DisplayablePaymentDetails?,
        ): LinkButtonState {
            val paymentUI = paymentDetails?.toDefaultPaymentUI(enableDefaultValuesInECE = false)
            return when {
                paymentUI != null -> DefaultPayment(paymentUI = paymentUI)
                linkEmail != null -> Email(email = linkEmail)
                else -> Default
            }
        }
    }
}
