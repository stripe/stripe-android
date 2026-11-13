package com.stripe.android.checkout.analytics

import androidx.test.core.app.ApplicationProvider
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.testing.FakeAnalyticsRequestExecutor

internal fun FakeCheckoutAnalyticsReporter(
    analyticsRequestExecutor: FakeAnalyticsRequestExecutor = FakeAnalyticsRequestExecutor(),
): CheckoutAnalyticsReporter {
    return CheckoutAnalyticsReporter(
        analyticsRequestExecutor = analyticsRequestExecutor,
        paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
            context = ApplicationProvider.getApplicationContext(),
            publishableKey = "pk_test_123",
        ),
    )
}
