package com.stripe.android.connect.di

import com.stripe.android.connect.BuildConfig
import com.stripe.android.connect.analytics.ConnectAnalyticsModule
import com.stripe.android.connect.analytics.ConnectAnalyticsServiceFactory
import com.stripe.android.connect.manager.EmbeddedComponentManagerComponent
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        StripeConnectComponentModule::class,
        CoreCommonModule::class,
        ConnectAnalyticsModule::class,
    ]
)
internal interface StripeConnectComponent {

    val analyticsServiceFactory: ConnectAnalyticsServiceFactory

    val coordinatorComponentProvider: Provider<EmbeddedComponentManagerComponent.Factory>

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun loggingEnabled(@Named(ENABLE_LOGGING) enabled: Boolean): Builder

        fun build(): StripeConnectComponent
    }

    companion object {
        internal var instance: StripeConnectComponent =
            DaggerStripeConnectComponent.builder()
                .loggingEnabled(BuildConfig.DEBUG)
                .build()
    }
}

@Module(subcomponents = [EmbeddedComponentManagerComponent::class])
internal class StripeConnectComponentModule
