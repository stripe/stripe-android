package com.stripe.android.connect.analytics

import dagger.Binds
import dagger.Module

@Module
internal interface ConnectAnalyticsModule {
    @Binds
    fun bindConnectAnalyticsServiceFactory(
        impl: DefaultConnectAnalyticsService.Factory
    ): ConnectAnalyticsServiceFactory
}
