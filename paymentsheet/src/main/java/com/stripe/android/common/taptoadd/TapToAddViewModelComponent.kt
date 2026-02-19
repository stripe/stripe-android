package com.stripe.android.common.taptoadd

import android.app.Application
import android.content.Context
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.PaymentConfiguration
import com.stripe.android.common.di.ApplicationIdModule
import com.stripe.android.common.taptoadd.ui.DefaultTapToAddCardAddedInteractor
import com.stripe.android.common.taptoadd.ui.DefaultTapToAddCollectingInteractor
import com.stripe.android.common.taptoadd.ui.DefaultTapToAddConfirmationInteractor
import com.stripe.android.common.taptoadd.ui.DefaultTapToAddLinkFormElementFactory
import com.stripe.android.common.taptoadd.ui.DefaultTapToAddLinkFormHelper
import com.stripe.android.common.taptoadd.ui.DefaultTapToAddPaymentMethodHolder
import com.stripe.android.common.taptoadd.ui.TapToAddCardAddedInteractor
import com.stripe.android.common.taptoadd.ui.TapToAddCollectingInteractor
import com.stripe.android.common.taptoadd.ui.TapToAddConfirmationInteractor
import com.stripe.android.common.taptoadd.ui.TapToAddLinkFormElementFactory
import com.stripe.android.common.taptoadd.ui.TapToAddLinkFormHelper
import com.stripe.android.common.taptoadd.ui.TapToAddPaymentMethodHolder
import com.stripe.android.common.taptoadd.ui.createTapToAddUxConfiguration
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DefaultDurationProvider
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.core.utils.RealUserFacingLogger
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.RealLinkConfigurationCoordinator
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.link.injection.LinkCommonModule
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.PaymentElementRequestSurfaceModule
import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.confirmation.ALLOWS_MANUAL_CONFIRMATION
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.injection.ConfirmationHandlerModule
import com.stripe.android.paymentelement.confirmation.injection.DefaultConfirmationModule
import com.stripe.android.paymentelement.confirmation.intent.DefaultIntentConfirmationModule
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.RealErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.stripeterminal.external.models.TapToPayUxConfiguration
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

@Component(
    modules = [
        ApplicationIdModule::class,
        CoreCommonModule::class,
        CoroutineContextModule::class,
        ConfirmationHandlerModule::class,
        DefaultConfirmationModule::class,
        DefaultIntentConfirmationModule::class,
        PaymentElementRequestSurfaceModule::class,
        TapToAddViewModelModule::class,
        TapToAddModule::class,
    ]
)
@Singleton
internal interface TapToAddViewModelComponent {
    val viewModel: TapToAddViewModel
    val subcomponentFactory: TapToAddSubcomponent.Factory

    @Component.Factory
    interface Factory {
        fun build(
            @BindsInstance paymentMethodMetadata: PaymentMethodMetadata,
            @BindsInstance tapToAddMode: TapToAddMode,
            @BindsInstance eventMode: EventReporter.Mode,
            @BindsInstance
            @PaymentElementCallbackIdentifier
            paymentElementCallbackIdentifier: String,
            @Named(PRODUCT_USAGE)
            @BindsInstance
            productUsage: Set<String>,
            @BindsInstance application: Application,
            @BindsInstance savedStateHandle: SavedStateHandle,
        ): TapToAddViewModelComponent
    }
}

@Module(
    subcomponents = [
        TapToAddSubcomponent::class,
    ],
    includes = [
        TapToAddLinkModule::class,
    ]
)
internal interface TapToAddViewModelModule {
    @Binds
    fun bindsErrorReporter(errorReporter: RealErrorReporter): ErrorReporter

    @Binds
    fun bindsPrefsRepositoryFactory(
        factory: DefaultPrefsRepository.Factory
    ): PrefsRepository.Factory

    @Binds
    fun bindsUserFacingLogger(
        userFacingLogger: RealUserFacingLogger
    ): UserFacingLogger

    @Binds
    fun bindsAnalyticsRequestFactory(
        paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory
    ): AnalyticsRequestFactory

    @Binds
    fun bindsPaymentMethodHolder(
        tapToAddPaymentMethodHolder: DefaultTapToAddPaymentMethodHolder
    ): TapToAddPaymentMethodHolder

    @Binds
    fun bindsEventReporter(
        eventReporter: DefaultEventReporter
    ): EventReporter

    @Binds
    fun bindsTapToAddCollectingInteractorFactory(
        tapToAddCollectingInteractorFactory: DefaultTapToAddCollectingInteractor.Factory
    ): TapToAddCollectingInteractor.Factory

    @Binds
    fun bindsTapToAddCardAddedInteractorFactory(
        tapToAddCardAddedInteractorFactory: DefaultTapToAddCardAddedInteractor.Factory
    ): TapToAddCardAddedInteractor.Factory

    @Binds
    fun bindsTapToAddConfirmationInteractorFactory(
        tapToAddConfirmationInteractorFactory: DefaultTapToAddConfirmationInteractor.Factory
    ): TapToAddConfirmationInteractor.Factory

    companion object {
        @Provides
        @Named(ENABLE_LOGGING)
        fun provideEnabledLogging(): Boolean = BuildConfig.DEBUG

        @Provides
        @Singleton
        @Named(ALLOWS_MANUAL_CONFIRMATION)
        fun provideAllowsManualConfirmation() = true

        @Provides
        @Singleton
        @Named(STATUS_BAR_COLOR)
        fun providesStatusBarColor(): Int? = null

        @Provides
        @Singleton
        fun providesTapToAddUxConfiguration(): TapToPayUxConfiguration {
            return createTapToAddUxConfiguration()
        }

        @Provides
        fun providesContext(application: Application): Context {
            return application
        }

        @Provides
        @Singleton
        fun provideConfirmationHandler(
            confirmationHandlerFactory: ConfirmationHandler.Factory,
            @ViewModelScope coroutineScope: CoroutineScope,
        ): ConfirmationHandler {
            return confirmationHandlerFactory.create(coroutineScope)
        }

        @Provides
        @Singleton
        fun provideDurationProvider(): DurationProvider {
            return DefaultDurationProvider.instance
        }

        @Provides
        @Singleton
        @ViewModelScope
        fun provideViewModelScope(): CoroutineScope {
            return CoroutineScope(Dispatchers.Main)
        }

        @Provides
        fun providePaymentConfiguration(appContext: Context): PaymentConfiguration {
            return PaymentConfiguration.getInstance(appContext)
        }

        @Provides
        @Named(PUBLISHABLE_KEY)
        fun providePublishableKey(
            paymentConfiguration: Provider<PaymentConfiguration>
        ): () -> String = { paymentConfiguration.get().publishableKey }

        @Provides
        @Named(STRIPE_ACCOUNT_ID)
        fun provideStripeAccountId(
            paymentConfiguration: Provider<PaymentConfiguration>
        ): () -> String? = { paymentConfiguration.get().stripeAccountId }

        @OptIn(ExperimentalAnalyticEventCallbackApi::class)
        @Provides
        fun providesAnalyticEventCallback(
            @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String,
        ): AnalyticEventCallback? {
            return PaymentElementCallbackReferences[paymentElementCallbackIdentifier]?.analyticEventCallback
        }
    }
}

@Module(
    subcomponents = [
        LinkAnalyticsComponent::class,
        LinkComponent::class,
    ],
    includes = [
        LinkCommonModule::class,
    ]
)
internal interface TapToAddLinkModule {
    @Binds
    fun bindsLinkConfigurationCoordinator(
        linkConfigurationCoordinator: RealLinkConfigurationCoordinator
    ): LinkConfigurationCoordinator

    @Binds
    fun bindsTapToAddLinkFormHelper(
        tapToAddLinkFormHelper: DefaultTapToAddLinkFormHelper
    ): TapToAddLinkFormHelper

    companion object {
        @Provides
        @Singleton
        fun providesLinkAccountHolder(savedStateHandle: SavedStateHandle): LinkAccountHolder {
            return LinkAccountHolder(savedStateHandle)
        }

        @Provides
        @Singleton
        fun providesTapToAddLinkFormElementFactory(
            savedStateHandle: SavedStateHandle
        ): TapToAddLinkFormElementFactory {
            return DefaultTapToAddLinkFormElementFactory
        }
    }
}

@TapToAddScope
@Subcomponent
internal interface TapToAddSubcomponent {
    fun inject(activity: TapToAddActivity)

    @Subcomponent.Factory
    interface Factory {
        fun build(
            @BindsInstance activityResultCaller: ActivityResultCaller,
            @BindsInstance lifecycleOwner: LifecycleOwner,
        ): TapToAddSubcomponent
    }
}
