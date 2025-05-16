package com.stripe.android.paymentelement.confirmation.link

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class LinkConfirmationOption(
    val configuration: LinkConfiguration,
    /**
     * The default payment method to be used for the payment.
     *
     * If provided and the user is authenticated, this payment method will be eagerly used
     * for confirmation.
     */
    val defaultLinkPayment: ConsumerPaymentDetails.PaymentDetails?,
    val useLinkExpress: Boolean
) : ConfirmationHandler.Option
