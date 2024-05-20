package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.payments.paymentlauncher.PaymentResult
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Handler used to respond to external payment method confirm results.
 */
object ExternalPaymentMethodResultHandler {

    /**
     * Updates the PaymentSheet UI to reflect the result of confirming an external payment method.
     *
     * Should be called when [ExternalPaymentMethodConfirmHandler.confirmExternalPaymentMethod] completes.
     */
    @JvmStatic
    fun onExternalPaymentMethodResult(context: Context, externalPaymentMethodResult: ExternalPaymentMethodResult) {
        context.startActivity(createResultIntent(context, externalPaymentMethodResult))
    }

    internal fun createResultIntent(
        context: Context,
        externalPaymentMethodResult: ExternalPaymentMethodResult
    ): Intent {
        return Intent().setClass(context, ExternalPaymentMethodProxyActivity::class.java)
            // Needed so that we can start the activity even if the context provided is an application context.
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Needed so that we can return to ExternalPaymentMethodActivity even if a merchant external payment
            // activity hasn't yet called finish()
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // Needed so that ExternalPaymentMethodActivity#onNewIntent is called
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_EXTERNAL_PAYMENT_METHOD_RESULT, externalPaymentMethodResult)
    }

    internal const val EXTRA_EXTERNAL_PAYMENT_METHOD_RESULT = "external_payment_method_result"
}

/**
 * The result of an attempt to confirm an external payment method.
 */
sealed class ExternalPaymentMethodResult : Parcelable {

    internal abstract fun toPaymentResult(): PaymentResult

    @Parcelize
    internal data object Completed : ExternalPaymentMethodResult() {
        override fun toPaymentResult(): PaymentResult = PaymentResult.Completed

        @IgnoredOnParcel
        const val RESULT_CODE = Activity.RESULT_OK
    }

    @Parcelize
    internal data object Canceled : ExternalPaymentMethodResult() {
        override fun toPaymentResult(): PaymentResult = PaymentResult.Canceled

        @IgnoredOnParcel
        const val RESULT_CODE = Activity.RESULT_CANCELED
    }

    @Parcelize
    internal data class Failed(
        val displayMessage: String?,
    ) : ExternalPaymentMethodResult() {
        override fun toPaymentResult(): PaymentResult = PaymentResult.Failed(
            LocalStripeException(
                displayMessage = displayMessage,
                analyticsValue = "externalPaymentMethodFailure"
            )
        )

        companion object {
            const val RESULT_CODE = Activity.RESULT_FIRST_USER
            const val DISPLAY_MESSAGE_EXTRA = "external_payment_method_error_message"
        }
    }

    companion object {

        /**
         * The customer successfully completed the payment or setup.
         */
        @JvmStatic
        fun completed(): ExternalPaymentMethodResult {
            return Completed
        }

        /**
         * The customer canceled the payment or setup attempt.
         */
        @JvmStatic
        fun canceled(): ExternalPaymentMethodResult {
            return Canceled
        }

        /**
         * The payment or setup attempt failed.
         *
         * @param displayMessage Message to display to the user on failure. If null, will display Stripe's default
         * error message.
         */
        @JvmStatic
        @JvmOverloads
        fun failed(displayMessage: String? = null): ExternalPaymentMethodResult {
            return Failed(displayMessage)
        }
    }
}
