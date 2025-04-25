package com.stripe.android.common.di

import com.stripe.android.core.networking.AnalyticsRequestFactory
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
internal class MobileSessionIdModule {
    @Provides
    @Named(MOBILE_SESSION_ID)
    fun mobileSessionIdProvider(): String {
        return AnalyticsRequestFactory.sessionId.toString()
    }
}

internal const val MOBILE_SESSION_ID = "mobile_session_id"
