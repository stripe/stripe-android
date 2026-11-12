package com.stripe.android.paymentsheet.repositories

import com.stripe.android.core.networking.AnalyticsRequestFactory

internal object CheckoutStripeRepositoryFactory {
    fun clientParams() = ElementsSessionClientParams(
        mobileAppId = "com.stripe.android.paymentsheet.test",
        mobileSessionIdProvider = { AnalyticsRequestFactory.sessionId.toString() },
    )
}
