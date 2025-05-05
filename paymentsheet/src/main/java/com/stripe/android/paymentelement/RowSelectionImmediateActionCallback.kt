package com.stripe.android.paymentelement

/**
 * Called when user sets [EmbeddedPaymentElement.RowSelectionBehavior] to ImmediateAction and the customer
 * selects a payment method.
 */
fun interface RowSelectionImmediateActionCallback {
    fun didSelectPaymentOption()
}
