package com.stripe.android.model

import com.stripe.android.CardNumberFixtures
import com.stripe.android.view.CardInputWidget

internal object CardParamsFixtures {
    @JvmField
    val MINIMUM = CardParams(
        CardNumberFixtures.VISA_NO_SPACES,
        1,
        2050,
        "123"
    )

    val DEFAULT = CardParams(
        number = CardNumberFixtures.VISA_NO_SPACES,
        expMonth = 12,
        expYear = 2045,
        cvc = "123",
        name = "Jenny Rosen",
        address = AddressFixtures.ADDRESS,
        currency = "usd",
        metadata = mapOf("fruit" to "orange")
    )

    val WITH_ATTRIBUTION = CardParams(
        brand = CardBrand.Visa,
        loggingTokens = setOf(CardInputWidget.LOGGING_TOKEN),
        number = CardNumberFixtures.VISA_NO_SPACES,
        expMonth = 12,
        expYear = 2045,
        cvc = "123"
    )
}
