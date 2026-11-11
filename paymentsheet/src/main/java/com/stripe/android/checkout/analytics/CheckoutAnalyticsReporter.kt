package com.stripe.android.checkout.analytics

import android.content.Context
import com.stripe.android.checkout.injection.DaggerCheckoutAnalyticsReporterComponent
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

    companion object {
        fun createFallbackInstance(context: Context): CheckoutAnalyticsReporter {
            return DaggerCheckoutAnalyticsReporterComponent.factory()
                .create(context.applicationContext)
                .analyticsReporter
        }
    }
}
