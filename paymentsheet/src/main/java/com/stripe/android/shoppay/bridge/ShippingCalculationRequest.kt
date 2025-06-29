package com.stripe.android.shoppay.bridge

import android.os.Parcelable
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class ShippingCalculationRequest(
    val shippingAddress: ShippingAddress,
) : StripeModel {
    @Parcelize
    data class ShippingAddress(
        val name: String?,
        val address: ECEPartialAddress,
    ) : Parcelable
}
