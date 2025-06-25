package com.stripe.android.shoppay.bridge

internal object ShopPayTestFactory {
    val ECE_SHIPPING_RATE = ECEShippingRate(
        id = "rate_1",
        displayName = "Standard Shipping",
        amount = 500,
        deliveryEstimate = ECEDeliveryEstimate.Text("5-7 business days")
    )
}
