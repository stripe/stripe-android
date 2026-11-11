package com.stripe.android.checkout.analytics

import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.networking.PaymentAnalyticsRequestFactory

internal class CheckoutAnalyticsReporter(
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
            return CheckoutAnalyticsReporter(
                analyticsRequestExecutor = DefaultAnalyticsRequestExecutor(),
                paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                    context = context,
                    defaultProductUsageTokens = emptySet(),
                    apiConfigurationProvider = {
                        val paymentConfiguration = PaymentConfiguration.getInstance(context)
                        ApiConfiguration.State(
                            publishableKey = paymentConfiguration.publishableKey,
                            stripeAccountId = paymentConfiguration.stripeAccountId,
                        )
                    },
                ),
            )
        }
    }
}
