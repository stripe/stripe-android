package com.stripe.android.link.verification

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.uicore.elements.OTPElement
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for handling Link logic when embedded in a Stripe UI component.
 *
 * @see [com.stripe.android.paymentsheet.ui.WalletButtonsInteractor.WalletButton]
 */
internal interface LinkInlineInteractor {

    val state: StateFlow<LinkInlineState>

    val otpElement: OTPElement

    /**
     * Sets up Link domain logic (should be called once when initializing).
     */
    fun setup(paymentMethodMetadata: PaymentMethodMetadata)

    fun resendCode()

    fun didShowCodeSentNotification()
}
