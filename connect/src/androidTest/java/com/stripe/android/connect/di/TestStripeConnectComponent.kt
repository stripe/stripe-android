package com.stripe.android.connect.di

import com.stripe.android.connect.analytics.TestConnectAnalyticsModule
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        StripeConnectComponentModule::class,
        CoreCommonModule::class,
        // Test modules.
        TestStripeConnectComponent.TestModule::class,
        TestConnectAnalyticsModule::class,
    ]
)
internal interface TestStripeConnectComponent : StripeConnectComponent {
    @Module
    class TestModule {
        @Provides
        @Named(ENABLE_LOGGING)
        fun providesLoggingEnabled(): Boolean = true
    }
}
