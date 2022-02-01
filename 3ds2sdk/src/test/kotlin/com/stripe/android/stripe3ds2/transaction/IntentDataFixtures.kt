package com.stripe.android.stripe3ds2.transaction

internal object IntentDataFixtures {
    val DEFAULT = IntentData(
        clientSecret = "pi_123_secret_456",
        sourceId = "src_12345",
        publishableKey = "pk_123"
    )
}
