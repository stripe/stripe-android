package com.stripe.android.common.model

import com.stripe.android.elements.payment.PaymentSheet

internal val SHOP_PAY_CONFIGURATION = shopPayConfiguration()

fun shopPayConfiguration(
    shippingAddressRequired: Boolean = true
): PaymentSheet.ShopPayConfiguration {
    return PaymentSheet.ShopPayConfiguration(
        shopId = "shop1234",
        billingAddressRequired = true,
        emailRequired = true,
        shippingAddressRequired = shippingAddressRequired,
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
        ),
        allowedShippingCountries = listOf("US", "CA")
    )
}
