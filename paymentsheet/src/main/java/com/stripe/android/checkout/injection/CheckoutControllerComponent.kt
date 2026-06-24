package com.stripe.android.checkout.injection

import android.app.Application
import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.common.di.ElementsSessionClientParamsModule
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.StripeNetworkClientModule
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.PaymentConfigurationModule
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Provider

@Component(
    modules = [
        CheckoutControllerModule::class,
        ElementsSessionClientParamsModule::class,
        CoreCommonModule::class,
        CoroutineContextModule::class,
    ],
)
internal interface CheckoutControllerComponent {
    val checkoutSessionRepository: CheckoutSessionRepository
    val analyticsRequestExecutor: AnalyticsRequestExecutor
    val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance application: Application): CheckoutControllerComponent
    }
}

@Module(includes = [PaymentConfigurationModule::class, StripeNetworkClientModule::class])
internal abstract class CheckoutControllerModule {
    @Binds
    abstract fun bindsAnalyticsRequestExecutor(
        default: DefaultAnalyticsRequestExecutor
    ): AnalyticsRequestExecutor

    companion object {
        @Provides
        fun provideContext(application: Application): Context = application.applicationContext

        @Provides
        @Named(ENABLE_LOGGING)
        fun provideEnabledLogging(): Boolean = BuildConfig.DEBUG

        @Provides
        @Named(PRODUCT_USAGE)
        fun provideProductUsageTokens(): Set<String> = setOf("CheckoutController")

        @Provides
        fun provideApiRequestOptions(
            paymentConfiguration: Provider<PaymentConfiguration>
        ): ApiRequest.Options = ApiRequest.Options(
            apiKey = paymentConfiguration.get().publishableKey,
            stripeAccount = paymentConfiguration.get().stripeAccountId,
        )
    }
}
