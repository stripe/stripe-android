@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.checkout.analytics.CheckoutOperationReporter
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.ApiConfigurationPreview
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext
import kotlin.time.TimeSource

@OptIn(ApiConfigurationPreview::class)
internal object TestCheckoutOperationReporterFactory {
    fun create(
        analyticsRequestExecutor: AnalyticsRequestExecutor,
        workContext: CoroutineContext,
        publishableKey: () -> String = { PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG.publishableKey },
        configurationParams: () -> Map<String, Any?> = { emptyMap() },
        apiConfiguration: ApiConfiguration.State = PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG,
        timeSource: TimeSource = TimeSource.Monotonic,
    ): CheckoutOperationReporter = CheckoutOperationReporter(
        context = ApplicationProvider.getApplicationContext<Context>(),
        apiConfigurationProvider = Provider { apiConfiguration },
        analyticsRequestExecutor = analyticsRequestExecutor,
        timeSource = timeSource,
        workContext = workContext,
        publishableKey = publishableKey,
        configurationParams = configurationParams,
    )
}
