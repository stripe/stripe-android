package com.stripe.android.link

import android.os.Parcelable
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.ui.core.forms.convertToFormValuesMap
import kotlinx.parcelize.Parcelize

/**
 * The payment method selected by the user within their Link account, including the parameters
 * needed to confirm the Stripe Intent.
 *
 * @param paymentDetails The [ConsumerPaymentDetails.PaymentDetails] selected by the user
 * @param paymentMethodCreateParams The [PaymentMethodCreateParams] to be used to confirm
 *                                  the Stripe Intent.
 */
sealed class LinkPaymentDetails(
    open val paymentDetails: ConsumerPaymentDetails.PaymentDetails,
    open val paymentMethodCreateParams: PaymentMethodCreateParams
) : Parcelable {

    /**
     * A [ConsumerPaymentDetails.PaymentDetails] that is already saved to the consumer's account.
     */
    @Parcelize
    internal class Saved(
        override val paymentDetails: ConsumerPaymentDetails.PaymentDetails,
        override val paymentMethodCreateParams: PaymentMethodCreateParams
    ) : LinkPaymentDetails(paymentDetails, paymentMethodCreateParams)

    /**
     * A new [ConsumerPaymentDetails.PaymentDetails], whose data was just collected from the user.
     * Must hold the original [PaymentMethodCreateParams] too in case we need to populate the form
     * fields with the user-entered values.
     */
    @Parcelize
    internal class New(
        override val paymentDetails: ConsumerPaymentDetails.PaymentDetails,
        override val paymentMethodCreateParams: PaymentMethodCreateParams,
        private val originalParams: PaymentMethodCreateParams
    ) : LinkPaymentDetails(paymentDetails, paymentMethodCreateParams) {

        /**
         * Build a flat map of the values entered by the user when creating this payment method,
         * in a format that can be used to set the initial values in the FormController.
         */
        fun buildFormValues() = convertToFormValuesMap(originalParams.toParamMap())
    }
}
