package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.common.taptoadd.TapToAddConnectionModule
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DefaultDurationProvider
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.link.injection.PaymentsIntegrityModule
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.PaymentElementRequestSurfaceModule
import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.confirmation.ALLOWS_MANUAL_CONFIRMATION
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.RealErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.PaymentConfigurationModule
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.LoadingEventReporter
import com.stripe.android.paymentsheet.repositories.CustomerApiRepository
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.DefaultSavedPaymentMethodRepository
import com.stripe.android.paymentsheet.repositories.SavedPaymentMethodRepository
import com.stripe.android.uicore.utils.mapAsStateFlow
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module(
    includes = [
        StripeRepositoryModule::class,
        CoreCommonModule::class,
        TapToAddConnectionModule::class,
        PaymentsIntegrityModule::class,
        PaymentElementRequestSurfaceModule::class,
        PaymentConfigurationModule::class,
    ],
)
internal interface EmbeddedCommonModule {
    @Binds
    @Singleton
    fun bindsEventReporter(eventReporter: DefaultEventReporter): EventReporter

    @Binds
    @Singleton
    fun bindsLoadingReporter(eventReporter: DefaultEventReporter): LoadingEventReporter

    @Binds
    fun bindsErrorReporter(errorReporter: RealErrorReporter): ErrorReporter

    @Binds
    fun bindsCustomerRepository(repository: CustomerApiRepository): CustomerRepository

    @Binds
    fun bindsSavedPaymentMethodRepository(
        repository: DefaultSavedPaymentMethodRepository,
    ): SavedPaymentMethodRepository

    @Binds
    fun bindsPaymentAnalyticsRequestFactory(
        paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory
    ): AnalyticsRequestFactory

    companion object {
        @Provides
        @Named(ENABLE_LOGGING)
        fun provideEnabledLogging(): Boolean = BuildConfig.DEBUG

        @Provides
        @IOContext
        fun ioContext(): CoroutineContext {
            return Dispatchers.IO
        }

        @Provides
        fun provideEventReporterMode(): EventReporter.Mode {
            return EventReporter.Mode.Embedded
        }

        @Provides
        @Named(PRODUCT_USAGE)
        fun provideProductUsageTokens() = setOf("EmbeddedPaymentElement")

        @Provides
        fun provideDurationProvider(): DurationProvider {
            return DefaultDurationProvider.instance
        }

        @Provides
        @Singleton
        @Named(ALLOWS_MANUAL_CONFIRMATION)
        fun provideAllowsManualConfirmation() = true

        @Provides
        @Singleton
        fun provideCustomerStateHolder(
            savedStateHandle: SavedStateHandle,
            selectionHolder: EmbeddedSelectionHolder,
            paymentMethodMetadataFlow: StateFlow<PaymentMethodMetadata?>
        ): CustomerStateHolder {
            val customerMetadata = paymentMethodMetadataFlow.mapAsStateFlow {
                it?.customerMetadata
            }
            return DefaultCustomerStateHolder(
                savedStateHandle = savedStateHandle,
                selection = selectionHolder.selection,
                customerMetadata = customerMetadata
            )
        }

        @Provides
        @Singleton
        @UIContext
        fun provideUiContext(): CoroutineContext = Dispatchers.Main

        @OptIn(ExperimentalAnalyticEventCallbackApi::class)
        @Provides
        fun providesAnalyticEventCallback(
            @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String,
        ): AnalyticEventCallback? {
            return PaymentElementCallbackReferences[paymentElementCallbackIdentifier]?.analyticEventCallback
        }
    }
}
