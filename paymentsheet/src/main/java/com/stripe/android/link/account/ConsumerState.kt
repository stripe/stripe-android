package com.stripe.android.link.account

import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerShippingAddress

internal data class ConsumerState(
    val paymentDetails: List<ConsumerPaymentDetails.PaymentDetails>,
    val shippingAddresses: List<ConsumerShippingAddress>,
)
