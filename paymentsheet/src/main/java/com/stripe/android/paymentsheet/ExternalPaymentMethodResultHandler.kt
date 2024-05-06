package com.stripe.android.paymentsheet

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Handler used to respond to external payment method confirm results.
 */
object ExternalPaymentMethodResultHandler {

    /**
     * Updates the PaymentSheet/FlowController UI to reflect the result of confirming an external payment method.
     *
     * Should be called when [ExternalPaymentMethodConfirmHandler.confirmExternalPaymentMethod] completes.     *
     */
    fun onExternalPaymentMethodResult(context: Context, externalPaymentMethodResult: ExternalPaymentMethodResult) {
        // TODO: store static var which says which integration is being used and start the correct class based on that.
        context.startActivity(
            Intent().setClass(context, PaymentSheetActivity::class.java)
                .putExtra("external_payment_method_result", externalPaymentMethodResult)
        )
    }
}


/**
 * The result of an attempt to confirm an external payment method.
 */
sealed interface ExternalPaymentMethodResult {

    /**
     * The customer successfully completed the payment or setup.
     */
    data object Completed : ExternalPaymentMethodResult

    /**
     * The customer canceled the payment or setup attempt.
     */
    data object Canceled : ExternalPaymentMethodResult

    /**
     * The payment or setup attempt failed.
     *
     * @param errorMessage Error message to display to the user on failure. If null, will display Stripe's default
     * error message.
     */
    data class Failed(val errorMessage: String?) : ExternalPaymentMethodResult

    companion object {
        fun completed(): ExternalPaymentMethodResult {
            return Completed
        }

        fun canceled(): ExternalPaymentMethodResult {
            return Canceled
        }

        fun failed(errorMessage: String? = null): ExternalPaymentMethodResult {
            return Failed(errorMessage = errorMessage)
        }
    }
}