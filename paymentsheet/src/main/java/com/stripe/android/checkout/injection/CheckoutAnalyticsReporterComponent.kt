package com.stripe.android.checkout.injection

import android.content.Context
import com.stripe.android.checkout.analytics.LocalOperation
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.payments.core.injection.ApiConfigurationFromPaymentConfigurationModule
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Component(
    modules = [
        CheckoutAnalyticsReporterModule::class,
        CoroutineContextModule::class,
        ApiConfigurationFromPaymentConfigurationModule::class,
    ]
)
internal interface CheckoutAnalyticsReporterComponent {
    val localOperationFactory: LocalOperation.Factory

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance context: Context,
        ): CheckoutAnalyticsReporterComponent
    }
}

@Module
internal interface CheckoutAnalyticsReporterModule {
    @Binds
    fun bindAnalyticsRequestExecutor(
        executor: DefaultAnalyticsRequestExecutor,
    ): AnalyticsRequestExecutor

    companion object {
        @Provides
        fun provideLogger(): Logger = Logger.noop()

        @Provides
        @Named(PRODUCT_USAGE)
        fun provideProductUsage(): Set<String> = emptySet()
    }
}
