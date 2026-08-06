package com.stripe.android.utils

import android.os.Bundle
import com.stripe.android.paymentsheet.example.test.BuildConfig
import com.stripe.android.testing.StripeAndroidJUnitRunner
import com.stripe.android.testing.StripeRetryConfig

internal class PaymentSheetTestRunner : StripeAndroidJUnitRunner() {
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        StripeRetryConfig.set(
            retryCount = 3,
            enabled = BuildConfig.IS_RUNNING_IN_CI && !BuildConfig.RUN_LATENCY_TESTS_IN_CI
        )
        configureLeakCanaryForManagedDevices()
    }
}
