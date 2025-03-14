package com.stripe.android.connect.di

import com.stripe.android.connect.BuildConfig
import com.stripe.android.connect.analytics.ConnectAnalyticsModule
import com.stripe.android.connect.analytics.ConnectAnalyticsServiceFactory
import com.stripe.android.connect.manager.EmbeddedComponentManagerComponent
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import org.jetbrains.annotations.TestOnly
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

    val logger: Logger

    val analyticsServiceFactory: ConnectAnalyticsServiceFactory

    val coordinatorComponentProvider: Provider<EmbeddedComponentManagerComponent.Factory>

    @Component.Factory
    interface Factory {
        fun build(
            @BindsInstance
            @Named(ENABLE_LOGGING)
            enableLogging: Boolean,
        ): StripeConnectComponent
    }

    companion object {
        private var _instance: StripeConnectComponent? = null

        internal val instance: StripeConnectComponent
            get() = synchronized(StripeConnectComponent) {
                _instance
                    ?: DaggerStripeConnectComponent.factory()
                        .build(enableLogging = BuildConfig.DEBUG)
                        .also { _instance = it }
            }

        @TestOnly
        internal fun replaceInstance(instance: StripeConnectComponent) {
            _instance = instance
        }
    }
}

@Module(subcomponents = [EmbeddedComponentManagerComponent::class])
internal class StripeConnectComponentModule
