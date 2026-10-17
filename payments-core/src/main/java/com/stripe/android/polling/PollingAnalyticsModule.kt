package com.stripe.android.polling

import androidx.annotation.RestrictTo
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import dagger.Binds
import dagger.Module

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
interface PollingAnalyticsModule {

    @Binds
    fun bindsPollingAnalyticsEventReporter(
        impl: DefaultPollingAnalyticsEventReporter
    ): PollingAnalyticsEventReporter

    @Binds
    fun bindsAnalyticsRequestFactory(
        impl: PaymentAnalyticsRequestFactory
    ): AnalyticsRequestFactory
}
