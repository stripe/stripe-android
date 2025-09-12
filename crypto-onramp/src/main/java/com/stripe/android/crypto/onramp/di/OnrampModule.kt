package com.stripe.android.crypto.onramp.di

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.BuildConfig
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.crypto.onramp.analytics.OnrampAnalyticsService
import com.stripe.android.crypto.onramp.analytics.OnrampAnalyticsServiceImpl
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import com.stripe.android.link.LinkController
import com.stripe.android.networking.RequestSurface
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module(
    subcomponents = [OnrampPresenterComponent::class]
)
internal class OnrampModule {
    @Provides
    @Named(ENABLE_LOGGING)
    fun provideEnableLogging(): Boolean = BuildConfig.DEBUG

    @Provides
    fun provideAppContext(application: Application): Context = application.applicationContext

    @Provides
    fun provideStripeNetworkClient(
        @IOContext context: CoroutineContext,
        logger: Logger
    ): StripeNetworkClient = DefaultStripeNetworkClient(
        workContext = context,
        logger = logger
    )

    @Provides
    @Named(PUBLISHABLE_KEY)
    fun providePublishableKey(context: Context): () -> String =
        { PaymentConfiguration.getInstance(context).publishableKey }

    @Provides
    @Named(STRIPE_ACCOUNT_ID)
    fun provideStripeAccountId(context: Context): () -> String? =
        { PaymentConfiguration.getInstance(context).stripeAccountId }

    @Provides
    fun provideRequestSurface(): RequestSurface = RequestSurface.CryptoOnramp

    @Provides
    @Named(PRODUCT_USAGE)
    fun provideProductUsageTokens(): Set<String> = setOf("CryptoOnramp")

    @Provides
    fun provideCryptoApiRepository(
        stripeNetworkClient: StripeNetworkClient,
        stripeRepository: StripeRepository,
        linkController: LinkController,
        @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
        @Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: () -> String?,
    ): CryptoApiRepository {
        return CryptoApiRepository(
            stripeNetworkClient = stripeNetworkClient,
            stripeRepository = stripeRepository,
            publishableKeyProvider = publishableKeyProvider,
            stripeAccountIdProvider = stripeAccountIdProvider,
            apiVersion = ApiVersion.get().code,
            linkController = linkController,
            appInfo = Stripe.appInfo
        )
    }

    @Provides
    @Singleton
    fun provideLinkController(
        application: Application,
        savedStateHandle: SavedStateHandle
    ): LinkController {
        return LinkController.create(
            application = application,
            savedStateHandle = savedStateHandle,
            requestSurface = RequestSurface.CryptoOnramp
        )
    }

    @Provides
    fun provideOnrampAnalyticsServiceFactory(
        impl: OnrampAnalyticsServiceImpl.Factory
    ): OnrampAnalyticsService.Factory {
        return impl
    }
}
