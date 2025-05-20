package com.stripe.android.link

import android.os.Parcelable
import com.stripe.android.model.ConsumerPaymentDetails
import kotlinx.parcelize.Parcelize

/**
 * Link payment method payload needed to confirm the payment.
 */
@Parcelize
internal data class LinkPaymentMethod(
    val details: ConsumerPaymentDetails.PaymentDetails,
    val collectedCvc: String?
) : Parcelable
