package com.stripe.android.link

import android.os.Parcelable
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.ui.core.forms.convertToFormValuesMap
import kotlinx.parcelize.Parcelize

/**
 * The payment method selected by the user within their Link account, including the parameters
 * needed to confirm the Stripe Intent.
 *
 * @param paymentDetails The [ConsumerPaymentDetails.PaymentDetails] selected by the user
 */
internal sealed class LinkPaymentDetails(
    open val paymentDetails: ConsumerPaymentDetails.PaymentDetails
) : Parcelable {

    /**
     * A [ConsumerPaymentDetails.PaymentDetails] that is already saved to the Link consumer's account.
     *
     * @param paymentMethod The [PaymentMethod] object that represents the Link payment method stored in the user's
     *   consumer account.
     */
    @Parcelize
    class Passthrough(
        override val paymentDetails: ConsumerPaymentDetails.Passthrough,
        val paymentMethod: PaymentMethod,
    ) : LinkPaymentDetails(paymentDetails)

    /**
     * A new [ConsumerPaymentDetails.PaymentDetails], whose data was just collected from the user.
     * Must hold the original [PaymentMethodCreateParams] too in case we need to populate the form
     * fields with the user-entered values.
     *
     * @param confirmParams The [PaymentMethodCreateParams] to be used to confirm the Stripe Intent.
     * @param originalParams The original [PaymentMethodCreateParams] created from the customer's input.
     */
    @Parcelize
    class New(
        override val paymentDetails: ConsumerPaymentDetails.PaymentDetails,
        val confirmParams: PaymentMethodCreateParams,
        val originalParams: PaymentMethodCreateParams,
    ) : LinkPaymentDetails(paymentDetails) {

        /**
         * Build a flat map of the values entered by the user when creating this payment method,
         * in a format that can be used to set the initial values in the FormController.
         */
        fun buildFormValues() = convertToFormValuesMap(originalParams.toParamMap())
    }
}
