package com.stripe.android.checkout.injection

import android.content.Context
import com.stripe.android.checkout.analytics.LocalOperation
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
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
        fun provideProductUsage(): Set<String> = setOf("checkout_elements")

        // Static Checkout APIs have no session, and merchants aren't expected to initialize PaymentConfiguration.
        @Provides
        fun provideApiConfiguration(): ApiConfiguration.State {
            return ApiConfiguration.State(
                publishableKey = ApiRequest.Options.UNDEFINED_PUBLISHABLE_KEY,
                stripeAccountId = null,
            )
        }
    }
}
