package com.stripe.android.common.model

import com.stripe.android.elements.payment.ShopPayConfiguration

internal val SHOP_PAY_CONFIGURATION = shopPayConfiguration()

fun shopPayConfiguration(
    shippingAddressRequired: Boolean = true
): ShopPayConfiguration {
    return ShopPayConfiguration(
        shopId = "shop1234",
        billingAddressRequired = true,
        emailRequired = true,
        shippingAddressRequired = shippingAddressRequired,
        lineItems = listOf(
            ShopPayConfiguration.LineItem(
                name = "Potato",
                amount = 50
            )
        ),
        shippingRates = listOf(
            ShopPayConfiguration.ShippingRate(
                id = "1234",
                amount = 50,
                displayName = "Express",
                deliveryEstimate = ShopPayConfiguration.DeliveryEstimate.Text("2 business days")
            )
        ),
        allowedShippingCountries = listOf("US", "CA")
    )
}
