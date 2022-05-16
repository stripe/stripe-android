package com.stripe.android.link

import android.os.Parcelable
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.PaymentMethodCreateParams
import kotlinx.parcelize.Parcelize

/**
 * The payment method selected by the user within their Link account, including the parameters
 * needed to confirm the Stripe Intent.
 *
 * @param paymentDetails The [ConsumerPaymentDetails.PaymentDetails] selected by the user
 * @param paymentMethodCreateParams The [PaymentMethodCreateParams] to be used to confirm
 *                                  the Stripe Intent.
 */
@Parcelize
data class LinkPaymentDetails(
    val paymentDetails: ConsumerPaymentDetails.PaymentDetails,
    val paymentMethodCreateParams: PaymentMethodCreateParams
) : Parcelable
