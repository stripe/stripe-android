package com.stripe.android.crypto.onramp.di
import com.stripe.android.Stripe
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
class OnrampModule {
    @Provides
    fun provideStripeNetworkClient(): StripeNetworkClient = DefaultStripeNetworkClient()

    @Provides @Named(PUBLISHABLE_KEY)
    fun providePublishableKeyProvider(): () -> String = { "" }

    @Provides @Named(STRIPE_ACCOUNT_ID)
    fun provideStripeAccountIdProvider(): () -> String? = { "" }

    @Provides fun provideApiVersion(): String = StripeSdkVersion.VERSION

    @Provides fun provideAppInfo(): AppInfo? = Stripe.appInfo
}
