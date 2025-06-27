package com.stripe.android.shoppay

import com.stripe.android.common.model.SHOP_PAY_CONFIGURATION
import com.stripe.android.shoppay.bridge.ECEDeliveryEstimate
import com.stripe.android.shoppay.bridge.ECEShippingRate

internal object ShopPayTestFactory {
    val ECE_SHIPPING_RATE = ECEShippingRate(
        id = "rate_1",
        displayName = "Standard Shipping",
        amount = 500,
        deliveryEstimate = ECEDeliveryEstimate.Text("5-7 business days")
    )

    val SHOP_PAY_ARGS = ShopPayArgs(
        publishableKey = "pk_1234",
        shopPayConfiguration = SHOP_PAY_CONFIGURATION,
        customerSessionClientSecret = "css_test_123",
        businessName = "Test Business",
        paymentElementCallbackIdentifier = "paymentElementCallbackIdentifier",
        stripeAccountId = "acct_123"
    )
}
