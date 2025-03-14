package com.stripe.android.connect.di

import com.stripe.android.connect.BuildConfig
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.analytics.ConnectAnalyticsModule
import com.stripe.android.connect.analytics.ConnectAnalyticsServiceFactory
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        CoreCommonModule::class,
        ConnectAnalyticsModule::class,
    ]
)
@OptIn(PrivateBetaConnectSDK::class)
internal interface StripeConnectComponent {

    val analyticsServiceFactory: ConnectAnalyticsServiceFactory

    fun inject(embeddedComponentManager: EmbeddedComponentManager)

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
