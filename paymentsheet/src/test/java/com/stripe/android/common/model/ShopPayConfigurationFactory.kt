package com.stripe.android.common.model

import com.stripe.android.paymentsheet.PaymentSheet

internal val SHOP_PAY_CONFIGURATION = PaymentSheet.ShopPayConfiguration(
    shopId = "shop1234",
    billingAddressRequired = true,
    emailRequired = true,
    shippingAddressRequired = true,
    lineItems = listOf(
        PaymentSheet.ShopPayConfiguration.LineItem(
            name = "Potato",
            amount = 50
        )
    ),
    shippingRates = listOf(
        PaymentSheet.ShopPayConfiguration.ShippingRate(
            id = "1234",
            amount = 50,
            displayName = "Express",
            deliveryEstimate = PaymentSheet.ShopPayConfiguration.DeliveryEstimate.Text("2 business days")
        )
    )
)
