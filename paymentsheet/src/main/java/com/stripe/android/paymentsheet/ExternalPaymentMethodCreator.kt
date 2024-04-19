package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Context
import android.content.Intent

data class ExternalPaymentMethodInput(val name : String, val billingDetails: PaymentSheet.BillingDetails)

val EXTRA_ERROR_MESSAGE = "error_message"
val RESULT_OK = Activity.RESULT_OK
val RESULT_CANCELED = Activity.RESULT_CANCELED
val RESULT_FAILED = Activity.RESULT_FIRST_USER

interface ExternalPaymentMethodCreator {
    /** Intent to launch activity to handle payment for the EPM. Activity should return a result of
     * [OK, CANCELLED, FAILED]. If [FAILED], optionally include an [error_message] to show the user. */
    fun createIntent(context: Context, input : ExternalPaymentMethodInput) : Intent
}

