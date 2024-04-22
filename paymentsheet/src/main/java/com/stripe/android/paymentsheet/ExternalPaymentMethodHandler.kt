package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Context
import android.content.Intent

// TODO: add correct javadoc etc
interface ExternalPaymentMethodHandler {
    /** Intent to launch activity to handle payment for the EPM. Activity should return a result of
     * [OK, CANCELLED, FAILED]. If [FAILED], optionally include an [error_message] to show the user. */
    fun createIntent(context: Context, input : ExternalPaymentMethodInput) : Intent
}

data class ExternalPaymentMethodInput(val externalPaymentMethod : String, val billingDetails: PaymentSheet.BillingDetails)

enum class ExternalPaymentMethodResult(val resultCode: Int) {
    RESULT_SUCCESS(resultCode = Activity.RESULT_OK),
    RESULT_CANCELED(resultCode = Activity.RESULT_CANCELED),
    RESULT_FAILED(resultCode = Activity.RESULT_FIRST_USER)
}

enum class ExternalPaymentMethodExtras(val key: String) {
    EXTRA_ERROR_MESSAGE(key = "error_message")
}