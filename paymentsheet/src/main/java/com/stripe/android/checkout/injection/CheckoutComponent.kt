package com.stripe.android.checkout.injection

import android.app.Application
import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.common.di.ApplicationIdModule
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.StripeNetworkClientModule
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.networking.PaymentElementRequestSurfaceModule
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.PaymentConfigurationModule
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Provider

@Component(
    modules = [
        CheckoutModule::class,
        StripeRepositoryModule::class,
        CoreCommonModule::class,
        CoroutineContextModule::class,
        PaymentElementRequestSurfaceModule::class,
        ApplicationIdModule::class,
    ],
)
internal interface CheckoutComponent {
    val checkoutSessionRepository: CheckoutSessionRepository

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance application: Application,
        ): CheckoutComponent
    }
}

@Module(includes = [PaymentConfigurationModule::class, StripeNetworkClientModule::class])
internal object CheckoutModule {
    @Provides
    fun provideContext(application: Application): Context = application

    @Provides
    @Named(ENABLE_LOGGING)
    fun provideEnabledLogging(): Boolean = BuildConfig.DEBUG

    @Provides
    @Named(PRODUCT_USAGE)
    fun provideProductUsageTokens(): Set<String> = setOf("Checkout")

    @Provides
    fun provideApiRequestOptions(
        paymentConfiguration: Provider<PaymentConfiguration>
    ): ApiRequest.Options = ApiRequest.Options(
        apiKey = paymentConfiguration.get().publishableKey,
        stripeAccount = paymentConfiguration.get().stripeAccountId,
    )
}
