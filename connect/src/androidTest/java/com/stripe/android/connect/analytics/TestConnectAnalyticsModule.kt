package com.stripe.android.connect.analytics

import android.app.Application
import dagger.Module
import dagger.Provides

@Module
internal class TestConnectAnalyticsModule {
    @Provides
    fun provideConnectAnalyticsServiceFactory(): ConnectAnalyticsServiceFactory =
        object : ConnectAnalyticsServiceFactory {
            override fun create(application: Application): ConnectAnalyticsService {
                return FakeConnectAnalyticsService()
            }
        }
}
