package com.stripe.android.paymentsheet.model

import com.stripe.android.model.PaymentIntentFixtures

internal object FragmentConfigFixtures {

    val DEFAULT = FragmentConfig(
        paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        paymentMethods = emptyList(),
        isGooglePayReady = true,
        savedSelection = SavedSelection.None
    )
}
