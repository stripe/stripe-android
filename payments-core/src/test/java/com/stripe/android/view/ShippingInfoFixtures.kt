package com.stripe.android.view

import com.stripe.android.model.Address
import com.stripe.android.model.ShippingInformation

internal object ShippingInfoFixtures {

    val DEFAULT = ShippingInformation(
        Address.Builder()
            .setCity("San Francisco")
            .setCountry("US")
            .setLine1("123 Market St")
            .setLine2("#345")
            .setPostalCode("94107")
            .setState("CA")
            .build(),
        "Jenny Rosen",
        "(555) 555-5555"
    )
}
