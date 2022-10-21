package com.stripe.android.link.injection

import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.link.BuildConfig
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.ui.core.forms.resources.injection.ResourceRepositoryModule
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        LinkCommonModule::class,
        CoreCommonModule::class,
        StripeRepositoryModule::class,
        CoroutineContextModule::class,
        CoreCommonModule::class,
        ResourceRepositoryModule::class,
        LinkLauncherModule::class
    ]
)
internal interface LinkLauncherComponent {
    val linkPaymentLauncher: LinkPaymentLauncher

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun appContext(appContext: Context): Builder

        fun build(): LinkLauncherComponent
    }
}

@Module
class LinkLauncherModule {

    @Provides
    @Singleton
    @Named(PRODUCT_USAGE)
    fun provideProductUsageTokens() = setOf("LinkLauncher")

    @Provides
    @Named(PUBLISHABLE_KEY)
    fun providePublishableKey(
        appContext: Context
    ): () -> String = { PaymentConfiguration.getInstance(appContext).publishableKey }

    @Provides
    @Named(STRIPE_ACCOUNT_ID)
    fun provideStripeAccountId(
        appContext: Context
    ): () -> String? = { PaymentConfiguration.getInstance(appContext).stripeAccountId }

    @Provides
    @Singleton
    @Named(ENABLE_LOGGING)
    fun provideEnabledLogging(): Boolean = BuildConfig.DEBUG
}
