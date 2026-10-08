package com.stripe.android.link.injection

import android.content.Context
import com.stripe.android.DefaultFraudDetectionDataRepository
import com.stripe.android.Stripe
import com.stripe.android.core.Logger
import com.stripe.android.core.frauddetection.FraudDetectionDataRepository
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.link.analytics.DefaultLinkEventsReporter
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.repositories.LinkApiRepository
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.repository.ConsumersApiService
import com.stripe.android.repository.ConsumersApiServiceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module
internal interface LinkCommonModule {
    @Binds
    @Singleton
    fun bindLinkRepository(linkApiRepository: LinkApiRepository): LinkRepository

    @Binds
    @Singleton
    fun bindLinkEventsReporter(linkEventsReporter: DefaultLinkEventsReporter): LinkEventsReporter

    @Binds
    @IntoSet
    fun bindSharedFraudDetectionDataRepository(
        repository: FraudDetectionDataRepository,
    ): FraudDetectionDataRepository

    companion object {
        @Provides
        @Singleton
        fun provideFraudDetectionDataRepository(
            context: Context,
            @IOContext workContext: CoroutineContext,
        ): FraudDetectionDataRepository = DefaultFraudDetectionDataRepository(context, workContext)

        @Provides
        @Singleton
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
