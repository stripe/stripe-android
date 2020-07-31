package com.stripe.android.model

import com.stripe.android.CardNumberFixtures

internal object CardParamsFixture {
    val DEFAULT = CardParams(
        number = CardNumberFixtures.VISA_NO_SPACES,
        expMonth = 12,
        expYear = 2025,
        cvc = "123",
        name = "Jenny Rosen",
        address = AddressFixtures.ADDRESS,
        currency = "usd",
        metadata = mapOf("fruit" to "orange")
    )
}
