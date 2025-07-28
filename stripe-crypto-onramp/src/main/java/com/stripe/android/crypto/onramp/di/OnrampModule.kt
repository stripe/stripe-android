package com.stripe.android.crypto.onramp.di
import android.app.Application
import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.AppInfo
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.version.StripeSdkVersion
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
internal class OnrampModule {
    @Provides
    fun provideStripeNetworkClient(): StripeNetworkClient = DefaultStripeNetworkClient()

    @Provides
    fun provideAppContext(application: Application): Context = application.applicationContext

    @Provides
    fun providePaymentConfiguration(appContext: Context): PaymentConfiguration {
        return PaymentConfiguration.getInstance(appContext)
    }

    @Provides
    @Named(PUBLISHABLE_KEY)
    fun providePublishableKey(appContext: Context): () -> String = {
        PaymentConfiguration.getInstance(appContext).publishableKey
    }

    @Provides
    @Named(STRIPE_ACCOUNT_ID)
    fun provideStripeAccountIdProvider(appContext: Context): () -> String? = {
        PaymentConfiguration.getInstance(appContext).stripeAccountId
    }

    @Provides fun provideApiVersion(): String = ApiVersion.get().code

    @Provides fun provideAppInfo(): AppInfo? = Stripe.appInfo
}
