package com.stripe.android.paymentsheet

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.paymentlauncher.PaymentResult
import kotlinx.parcelize.Parcelize
import java.lang.IllegalStateException

/**
 * Handler used to respond to external payment method confirm results.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ExternalPaymentMethodResultHandler {

    /**
     * Updates the PaymentSheet UI to reflect the result of confirming an external payment method.
     *
     * Should be called when [ExternalPaymentMethodConfirmHandler.confirmExternalPaymentMethod] completes.
     */
    fun onExternalPaymentMethodResult(context: Context, externalPaymentMethodResult: ExternalPaymentMethodResult) {
        fun updatePaymentSheetWithResult() {
            context.startActivity(
                Intent().setClass(context, PaymentSheetActivity::class.java)
                    // Needed so that we can start the activity even if the context provided is an application context.
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    // Needed so that we can return to PaymentSheet even if a merchant external payment activity hasn't
                    // yet called finish()
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    // Needed so that PaymentSheetActivity#onNewIntent is called
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(EXTRA_EXTERNAL_PAYMENT_METHOD_RESULT, externalPaymentMethodResult)
            )
        }

        fun updateFlowControllerWithResult() {
            val flowControllerInstance = PaymentSheet.FlowController.instance
            if (flowControllerInstance == null) {
                val error = IllegalStateException(
                    "Attempting to confirm external payment method for FlowController without an instance."
                )
                ErrorReporter
                    .createFallbackInstance(context)
                    .report(ErrorReporter.UnexpectedErrorEvent.EXTERNAL_PAYMENT_METHOD_FLOW_CONTROLLER_INSTANCE_MISSING)
                throw error
            } else {
                flowControllerInstance.onExternalPaymentMethodResult(externalPaymentMethodResult.toPaymentResult())
            }
        }

        when (integrationType) {
            IntegrationType.PAYMENT_SHEET -> updatePaymentSheetWithResult()
            IntegrationType.FLOW_CONTROLLER -> updateFlowControllerWithResult()
        }
    }

    internal const val EXTRA_EXTERNAL_PAYMENT_METHOD_RESULT = "external_payment_method_result"
    internal enum class IntegrationType { PAYMENT_SHEET, FLOW_CONTROLLER }
    internal var integrationType: IntegrationType = IntegrationType.PAYMENT_SHEET
}

/**
 * The result of an attempt to confirm an external payment method.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class ExternalPaymentMethodResult : Parcelable {

    internal abstract fun toPaymentResult(): PaymentResult

    @Parcelize
    internal data object Completed : ExternalPaymentMethodResult() {
        override fun toPaymentResult(): PaymentResult = PaymentResult.Completed
    }

    @Parcelize
    internal data object Canceled : ExternalPaymentMethodResult() {
        override fun toPaymentResult(): PaymentResult = PaymentResult.Canceled
    }

    @Parcelize
    internal data class Failed(private val errorMessage: String?) : ExternalPaymentMethodResult() {
        override fun toPaymentResult(): PaymentResult = PaymentResult.Failed(
            LocalStripeException(
                displayMessage = errorMessage,
                analyticsValue = "externalPaymentMethodFailure"
            )
        )
    }

    companion object {

        /**
         * The customer successfully completed the payment or setup.
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun completed(): ExternalPaymentMethodResult {
            return Completed
        }

        /**
         * The customer canceled the payment or setup attempt.
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun canceled(): ExternalPaymentMethodResult {
            return Canceled
        }

        /**
         * The payment or setup attempt failed.
         *
         * @param errorMessage Error message to display to the user on failure. If null, will display Stripe's default
         * error message.
         */
        @JvmStatic
        @JvmOverloads
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun failed(errorMessage: String? = null): ExternalPaymentMethodResult {
            return Failed(errorMessage)
        }
    }
}
