package com.stripe.android.analytics

import android.content.Context
import com.stripe.android.BuildConfig
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.core.utils.DefaultDurationProvider
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import kotlinx.coroutines.Dispatchers

internal object PaymentSessionEventReporterFactory {
    fun create(context: Context): PaymentSessionEventReporter {
        val workContext = Dispatchers.IO
        val configuration = PaymentConfiguration.getInstance(context)

        return DefaultPaymentSessionEventReporter(
            analyticsRequestExecutor = DefaultAnalyticsRequestExecutor(
                logger = Logger.getInstance(BuildConfig.DEBUG),
                workContext = workContext
            ),
            paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                context = context,
                publishableKey = configuration.publishableKey
            ),
            durationProvider = DefaultDurationProvider.instance,
            workContext = Dispatchers.IO
        )
    }
}
