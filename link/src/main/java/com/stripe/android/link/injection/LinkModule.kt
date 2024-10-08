package com.stripe.android.link.injection

import com.stripe.android.Stripe
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.link.account.DefaultLinkAccountManager
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.DefaultLinkEventsReporter
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.repositories.LinkApiRepository
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.repository.ConsumersApiService
import com.stripe.android.repository.ConsumersApiServiceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlin.coroutines.CoroutineContext

@Module
internal interface LinkModule {
    @Binds
    @LinkScope
    fun bindLinkRepository(linkApiRepository: LinkApiRepository): LinkRepository

    @Binds
    @LinkScope
    fun bindLinkEventsReporter(linkEventsReporter: DefaultLinkEventsReporter): LinkEventsReporter

    @Binds
    @LinkScope
    fun bindLinkAccountManager(linkAccountManager: DefaultLinkAccountManager): LinkAccountManager

    companion object {
        @Provides
        @LinkScope
        fun provideConsumersApiService(
            logger: Logger,
            @IOContext workContext: CoroutineContext,
        ): ConsumersApiService = ConsumersApiServiceImpl(
            appInfo = Stripe.appInfo,
            sdkVersion = StripeSdkVersion.VERSION,
            apiVersion = Stripe.API_VERSION,
            stripeNetworkClient = DefaultStripeNetworkClient(
                logger = logger,
                workContext = workContext
            )
        )
    }
}
