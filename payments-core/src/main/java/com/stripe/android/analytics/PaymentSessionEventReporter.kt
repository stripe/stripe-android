package com.stripe.android.analytics

import com.stripe.android.model.PaymentMethodCode

internal interface PaymentSessionEventReporter {
    /**
     * Payment session has started loading
     */
    fun onLoadStarted()

    /**
     * Payment session was successfully loaded.
     */
    fun onLoadSucceeded(code: PaymentMethodCode?)

    /**
     * Payment session failed to load.
     */
    fun onLoadFailed(error: Throwable)

    /**
     * User was shown available payment options
     */
    fun onOptionsShown()

    /**
     * User was shown a payment form
     *
     * @param code payment method code for the form type shown to the user
     */
    fun onFormShown(code: PaymentMethodCode)

    /**
     * User interacted with the payment form
     *
     * @param code payment method code for the form type interacted by the user
     */
    fun onFormInteracted(code: PaymentMethodCode)

    /**
     * User completes entering card number in payment form
     */
    fun onCardNumberCompleted()

    /**
     * User clicks the done button to indicate completing the payment form
     *
     * @param code payment method code for the form type the user has completed
     */
    fun onDoneButtonTapped(code: PaymentMethodCode)
}
