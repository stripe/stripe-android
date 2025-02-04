package com.stripe.android.link.injection

import android.app.Application
import android.content.Context
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.BuildConfig
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.NetworkTypeDetector
import com.stripe.android.core.utils.ContextUtils.packageInfo
import com.stripe.android.core.utils.DefaultDurationProvider
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.link.account.DefaultLinkAccountManager
import com.stripe.android.link.account.DefaultLinkAuth
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.LinkAuth
import com.stripe.android.link.analytics.DefaultLinkEventsReporter
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.confirmation.DefaultLinkConfirmationHandler
import com.stripe.android.link.confirmation.LinkConfirmationHandler
import com.stripe.android.link.gate.DefaultLinkGate
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.repositories.LinkApiRepository
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.confirmation.ALLOWS_MANUAL_CONFIRMATION
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.RealErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.repository.ConsumersApiService
import com.stripe.android.repository.ConsumersApiServiceImpl
import com.stripe.attestation.IntegrityRequestManager
import com.stripe.attestation.IntegrityStandardRequestManager
import com.stripe.attestation.RealStandardIntegrityManagerFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

@Module
internal interface NativeLinkModule {
    @Binds
    @NativeLinkScope
    fun bindLinkRepository(linkApiRepository: LinkApiRepository): LinkRepository

    @Binds
    @NativeLinkScope
    fun bindLinkEventsReporter(linkEventsReporter: DefaultLinkEventsReporter): LinkEventsReporter

    @Binds
    @NativeLinkScope
    fun bindLinkAccountManager(linkAccountManager: DefaultLinkAccountManager): LinkAccountManager

    @Binds
    @NativeLinkScope
    fun bindsErrorReporter(errorReporter: RealErrorReporter): ErrorReporter

    @Binds
    @NativeLinkScope
    fun stripeRepository(stripeRepository: StripeApiRepository): StripeRepository

    @Binds
    @NativeLinkScope
    fun bindsCardAccountRangeRepositoryFactory(
        defaultCardAccountRangeRepositoryFactory: DefaultCardAccountRangeRepositoryFactory
    ): CardAccountRangeRepository.Factory

    @Binds
    @NativeLinkScope
    fun bindsEventReporter(eventReporter: DefaultEventReporter): EventReporter

    @Binds
    @NativeLinkScope
    fun bindsLinkGate(linkGate: DefaultLinkGate): LinkGate

    @Binds
    @NativeLinkScope
    fun bindsLinkAuth(linkGate: DefaultLinkAuth): LinkAuth

    @SuppressWarnings("TooManyFunctions")
    companion object {
        @Provides
        @NativeLinkScope
        fun providesLinkAccountHolder(
            savedStateHandle: SavedStateHandle,
            linkAccount: LinkAccount?
        ): LinkAccountHolder {
            return LinkAccountHolder(savedStateHandle).apply {
                set(linkAccount)
            }
        }

        @Provides
        @NativeLinkScope
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

        @Provides
        @NativeLinkScope
        fun provideAnalyticsRequestFactory(
            context: Context,
            @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String
        ): AnalyticsRequestFactory = AnalyticsRequestFactory(
            packageManager = context.packageManager,
            packageName = context.packageName.orEmpty(),
            packageInfo = context.packageInfo,
            publishableKeyProvider = publishableKeyProvider,
            networkTypeProvider = NetworkTypeDetector(context)::invoke,
        )

        @Provides
        @NativeLinkScope
        fun provideDurationProvider(): DurationProvider {
            return DefaultDurationProvider.instance
        }

        @Provides
        @NativeLinkScope
        fun provideLogger(@Named(ENABLE_LOGGING) enableLogging: Boolean) =
            Logger.getInstance(enableLogging)

        @Provides
        @NativeLinkScope
        fun provideLocale() =
            LocaleListCompat.getAdjustedDefault().takeUnless { it.isEmpty }?.get(0)

        @IOContext
        @Provides
        @NativeLinkScope
        fun ioContext(): CoroutineContext = Dispatchers.IO

        @Provides
        @NativeLinkScope
        @Named(PRODUCT_USAGE)
        fun provideProductUsageTokens() = setOf("PaymentSheet")

        @Provides
        @NativeLinkScope
        fun providesAnalyticsRequestExecutor(
            executor: DefaultAnalyticsRequestExecutor
        ): AnalyticsRequestExecutor = executor

        @Provides
        @Named(ENABLE_LOGGING)
        @NativeLinkScope
        fun providesEnableLogging(): Boolean = BuildConfig.DEBUG

        @Provides
        @NativeLinkScope
        fun providePaymentConfiguration(appContext: Context): PaymentConfiguration {
            return PaymentConfiguration.getInstance(appContext)
        }

        @Provides
        @NativeLinkScope
        @Named(ALLOWS_MANUAL_CONFIRMATION)
        fun provideAllowsManualConfirmation() = true

        @Provides
        @NativeLinkScope
        fun provideLinkConfirmationHandlerFactory(
            factory: DefaultLinkConfirmationHandler.Factory
        ): LinkConfirmationHandler.Factory = factory

        @Provides
        @NativeLinkScope
        fun providesIntegrityStandardRequestManager(
            context: Application
        ): IntegrityRequestManager = IntegrityStandardRequestManager(
            cloudProjectNumber = 577365562050, // stripe-payments-sdk-prod
            logError = { message, error ->
                Logger.getInstance(BuildConfig.DEBUG).error(message, error)
            },
            factory = RealStandardIntegrityManagerFactory(context)
        )

        @Provides
        @NativeLinkScope
        fun provideEventReporterMode(): EventReporter.Mode = EventReporter.Mode.Custom

        @Provides
        @NativeLinkScope
        @Named(APPLICATION_ID)
        fun provideApplicationId(
            application: Application
        ): String {
            return application.packageName
        }
    }
}
