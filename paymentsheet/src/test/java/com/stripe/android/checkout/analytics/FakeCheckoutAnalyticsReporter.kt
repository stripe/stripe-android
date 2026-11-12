package com.stripe.android.checkout.analytics

import androidx.test.core.app.ApplicationProvider
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.testing.FakeAnalyticsRequestExecutor

internal fun FakeCheckoutAnalyticsReporter(): CheckoutAnalyticsReporter {
    return CheckoutAnalyticsReporter(
        analyticsRequestExecutor = FakeAnalyticsRequestExecutor(),
        paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
            context = ApplicationProvider.getApplicationContext(),
            publishableKey = "pk_test_123",
        ),
    )
}
