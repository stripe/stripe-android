package com.stripe.android.paymentelement

import android.content.Context
import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

/**
 * Handler used to respond to custom payment method confirm results.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object CustomPaymentMethodResultHandler {

    /**
     * Updates the Payment Element UI to reflect the result of confirming a custom payment method.
     *
     * Should be called when [ConfirmCustomPaymentMethodCallback.onConfirmCustomPaymentMethod] completes.
     */
    @JvmStatic
    fun handleCustomPaymentMethodResult(context: Context, customPaymentMethodResult: CustomPaymentMethodResult) {
        error(
            "Not implemented! Should not called with context from " +
                "${context.packageName} and $customPaymentMethodResult"
        )
    }
}

/**
 * The result of an attempt to confirm a custom payment method.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class CustomPaymentMethodResult : Parcelable {
    @Parcelize
    internal data object Completed : CustomPaymentMethodResult()

    @Parcelize
    internal data object Canceled : CustomPaymentMethodResult()

    @Parcelize
    internal data class Failed(
        val displayMessage: String?,
    ) : CustomPaymentMethodResult()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {

        /**
         * The customer successfully completed the payment or setup.
         */
        @JvmStatic
        @ExperimentalCustomPaymentMethodsApi
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun completed(): CustomPaymentMethodResult {
            return Completed
        }

        /**
         * The customer canceled the payment or setup attempt.
         */
        @JvmStatic
        @ExperimentalCustomPaymentMethodsApi
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
        @ExperimentalCustomPaymentMethodsApi
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun failed(displayMessage: String? = null): CustomPaymentMethodResult {
            return Failed(displayMessage)
        }
    }
}
