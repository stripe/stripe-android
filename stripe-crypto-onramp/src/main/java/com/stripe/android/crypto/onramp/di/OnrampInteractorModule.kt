package com.stripe.android.crypto.onramp.di

import android.app.Application
import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import dagger.Module
import dagger.Provides

@Module
internal class OnrampInteractorModule {
    @Provides
    fun provideStripeNetworkClient(): StripeNetworkClient = DefaultStripeNetworkClient()

    @Provides
    fun provideAppContext(application: Application): Context = application.applicationContext

    @Provides
    fun providePaymentConfiguration(appContext: Context): PaymentConfiguration {
        return PaymentConfiguration.getInstance(appContext)
    }

    @Provides
    fun provideCryptoApiRepository(
        stripeNetworkClient: StripeNetworkClient,
        paymentConfiguration: PaymentConfiguration
    ): CryptoApiRepository {
        return CryptoApiRepository(
            stripeNetworkClient = stripeNetworkClient,
            publishableKeyProvider = { paymentConfiguration.publishableKey },
            stripeAccountIdProvider = { paymentConfiguration.stripeAccountId },
            apiVersion = ApiVersion.get().code,
            appInfo = Stripe.appInfo
        )
    }
}
