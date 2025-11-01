package com.stripe.android.link.injection

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata

internal data class LinkMetadata(
    val linkConfiguration: LinkConfiguration,
    val paymentMethodMetadata: PaymentMethodMetadata,
)
