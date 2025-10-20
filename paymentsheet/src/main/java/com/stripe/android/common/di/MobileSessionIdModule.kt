package com.stripe.android.common.di

import androidx.annotation.RestrictTo
import com.stripe.android.core.networking.AnalyticsRequestFactory
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class MobileSessionIdModule {
    @Provides
    @Named(MOBILE_SESSION_ID)
    fun mobileSessionIdProvider(): String {
        return AnalyticsRequestFactory.sessionId.toString()
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val MOBILE_SESSION_ID = "mobile_session_id"
