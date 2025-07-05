package com.stripe.android.link

import android.os.Parcelable
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import kotlinx.parcelize.Parcelize

/**
 * The payment method selected by the user within their Link account, including the parameters
 * needed to confirm the Stripe Intent.
 */
internal sealed interface LinkPaymentDetails : Parcelable {

    /**
     * The Link payment details used for confirmation in passthrough mode.
     *
     * @param paymentMethod The [PaymentMethod] of type 'card'
     */
    @Parcelize
    class ForPassthroughMode(
        val paymentMethod: PaymentMethod,
    ) : LinkPaymentDetails

    /**
     * The Link payment details used for confirmation in payment method mode.
     *
     * @param paymentDetails The [ConsumerPaymentDetails] representing the Link payment details
     * @param paymentMethodCreateParams The Link-specific payment method create params
     * @param originalParams The original payment method create params in case we need to populate the form
     * fields with the user-entered values.
     */
    @Parcelize
    data class ForPaymentMethodMode(
        val paymentDetails: ConsumerPaymentDetails.PaymentDetails,
        val paymentMethodCreateParams: PaymentMethodCreateParams,
        val originalParams: PaymentMethodCreateParams
    ) : LinkPaymentDetails
}
