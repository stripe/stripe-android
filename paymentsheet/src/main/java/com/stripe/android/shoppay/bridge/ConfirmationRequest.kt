package com.stripe.android.shoppay.bridge

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class ConfirmationRequest(
    val paymentDetails: ConfirmEventData
) : StripeModel {
    @Parcelize
    data class ConfirmEventData(
        val billingDetails: ECEBillingDetails,
        val shippingAddress: ECEShippingAddressData? = null,
        val shippingRate: ECEShippingRate? = null,
        val paymentMethodOptions: ECEPaymentMethodOptions? = null
    ) : StripeModel
}
