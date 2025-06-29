package com.stripe.android.shoppay.bridge

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class ShippingRateChangeRequest(
    val shippingRate: ECEShippingRate,
) : StripeModel
