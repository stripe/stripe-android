package com.stripe.android.paymentelement

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.paymentelement.confirmation.cpms.CustomPaymentMethodProxyActivity
import com.stripe.android.payments.paymentlauncher.PaymentResult
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Handler used to respond to custom payment method confirm results.
 */
@ExperimentalCustomPaymentMethodsApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object CustomPaymentMethodResultHandler {

    /**
     * Updates the Payment Element UI to reflect the result of confirming a custom payment method.
     *
     * Should be called when [CustomPaymentMethodConfirmHandler.confirmCustomPaymentMethod] completes.
     */
    @JvmStatic
    fun onCustomPaymentMethodResult(context: Context, customPaymentMethodResult: CustomPaymentMethodResult) {
        context.startActivity(createResultIntent(context, customPaymentMethodResult))
    }

    internal fun createResultIntent(
        context: Context,
        customPaymentMethodResult: CustomPaymentMethodResult,
    ): Intent {
        return Intent().setClass(context, CustomPaymentMethodProxyActivity::class.java)
            // Needed so that we can start the activity even if the context provided is an application context.
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Needed so that we can return to ExternalPaymentMethodActivity even if a merchant external payment
            // activity hasn't yet called finish()
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // Needed so that ExternalPaymentMethodActivity#onNewIntent is called
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_CUSTOM_PAYMENT_METHOD_RESULT, customPaymentMethodResult)
    }

    internal const val EXTRA_CUSTOM_PAYMENT_METHOD_RESULT = "custom_payment_method_result"
}

/**
 * The result of an attempt to confirm a custom payment method.
 */
@ExperimentalCustomPaymentMethodsApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class CustomPaymentMethodResult : Parcelable {

    internal abstract fun toPaymentResult(): PaymentResult

    @Parcelize
    internal data object Completed : CustomPaymentMethodResult() {
        override fun toPaymentResult(): PaymentResult = PaymentResult.Completed

        @IgnoredOnParcel
        const val RESULT_CODE = Activity.RESULT_OK
    }

    @Parcelize
    internal data object Canceled : CustomPaymentMethodResult() {
        override fun toPaymentResult(): PaymentResult = PaymentResult.Canceled

        @IgnoredOnParcel
        const val RESULT_CODE = Activity.RESULT_CANCELED
    }

    @Parcelize
    internal data class Failed(
        val displayMessage: String?,
    ) : CustomPaymentMethodResult() {
        override fun toPaymentResult(): PaymentResult = PaymentResult.Failed(
            LocalStripeException(
                displayMessage = displayMessage,
                analyticsValue = "customPaymentMethodFailure"
            )
        )

        companion object {
            const val RESULT_CODE = Activity.RESULT_FIRST_USER
            const val DISPLAY_MESSAGE_EXTRA = "custom_payment_method_error_message"
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {

        /**
         * The customer successfully completed the payment or setup.
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun completed(): CustomPaymentMethodResult {
            return Completed
        }

        /**
         * The customer canceled the payment or setup attempt.
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun canceled(): CustomPaymentMethodResult {
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
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun failed(displayMessage: String? = null): CustomPaymentMethodResult {
            return Failed(displayMessage)
        }
    }
}
