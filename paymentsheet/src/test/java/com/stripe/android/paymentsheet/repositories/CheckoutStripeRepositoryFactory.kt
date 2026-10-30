package com.stripe.android.paymentsheet.repositories

import androidx.test.core.app.ApplicationProvider
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.RequestSurface
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.utils.FakeFraudDetectionDataRepository
import kotlinx.coroutines.Dispatchers

internal object CheckoutStripeRepositoryFactory {
    fun create() = StripeApiRepository(
        appContext = ApplicationProvider.getApplicationContext(),
        apiConfigurationProvider = { DEFAULT_API_CONFIG },
        requestSurface = RequestSurface.PaymentElement,
        workContext = Dispatchers.IO,
        productUsageTokens = emptySet(),
        paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
            context = ApplicationProvider.getApplicationContext(),
            publishableKey = DEFAULT_API_CONFIG.publishableKey,
        ),
        analyticsRequestExecutor = FakeAnalyticsRequestExecutor(),
        logger = Logger.noop(),
        fraudDetectionDataRepository = FakeFraudDetectionDataRepository.create(),
    )

    fun clientParams() = ElementsSessionClientParams(
        mobileAppId = "com.stripe.android.paymentsheet.test",
        mobileSessionIdProvider = { AnalyticsRequestFactory.sessionId.toString() },
    )
}
