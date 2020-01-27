package com.stripe.android.model

internal object MandateDataParamsFixtures {
    val DEFAULT = MandateDataParams(
        MandateDataParams.TypeData.Online(
            ipAddress = "127.0.0.1",
            userAgent = "my_user_agent"
        )
    )
}
