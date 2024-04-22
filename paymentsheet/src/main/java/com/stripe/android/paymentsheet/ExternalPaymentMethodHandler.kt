package com.stripe.android.paymentsheet

import android.content.Context
import android.content.Intent

// TODO: add correct javadoc etc
interface ExternalPaymentMethodHandler {
    /** Intent to launch activity to handle payment for the EPM. Activity should return a result of
     * [OK, CANCELLED, FAILED]. If [FAILED], optionally include an [error_message] to show the user. */
    fun createIntent(context: Context, input : ExternalPaymentMethodInput) : Intent
}

data class ExternalPaymentMethodInput(val name : String, val billingDetails: PaymentSheet.BillingDetails)
