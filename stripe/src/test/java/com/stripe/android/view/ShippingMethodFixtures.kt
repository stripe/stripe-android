package com.stripe.android.view

import com.stripe.android.model.ShippingMethod

internal object ShippingMethodFixtures {
    val UPS = ShippingMethod(
        "UPS Ground",
        "ups-ground",
        0,
        "USD",
        "Arrives in 3-5 days"
    )
    val FEDEX = ShippingMethod(
        "FedEx",
        "fedex",
        599,
        "USD",
        "Arrives tomorrow"
    )
}
