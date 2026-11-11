package com.stripe.android.checkout.analytics

import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import javax.inject.Inject

internal class CheckoutAnalyticsReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
) {
    fun fireEvent(event: CheckoutAnalyticsEvent) {
        analyticsRequestExecutor.executeAsync(
            paymentAnalyticsRequestFactory.createRequest(
                event = event,
                additionalParams = event.params,
            )
        )
    }
}
