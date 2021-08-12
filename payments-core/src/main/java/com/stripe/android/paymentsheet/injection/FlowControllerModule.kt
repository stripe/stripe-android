package com.stripe.android.paymentsheet.injection

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.Logger
import com.stripe.android.PaymentConfiguration
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.payments.core.injection.ENABLE_LOGGING
import com.stripe.android.payments.core.injection.IOContext
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.FlowController
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.analytics.DefaultDeviceIdRepository
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.DeviceIdRepository
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent
import com.stripe.android.paymentsheet.flowcontroller.DefaultFlowControllerInitializer
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerInitializer
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerViewModel
import com.stripe.android.paymentsheet.model.ClientSecret
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module
internal abstract class FlowControllerModule {
    @Binds
    abstract fun bindsFlowControllerInitializer(
        defaultFlowControllerInitializer: DefaultFlowControllerInitializer
    ): FlowControllerInitializer

    @Binds
    abstract fun bindsEventReporter(eventReporter: DefaultEventReporter): EventReporter

    @Binds
    abstract fun bindsDeviceIdRepository(repository: DefaultDeviceIdRepository): DeviceIdRepository

    companion object {
        @Provides
        @Named(ENABLE_LOGGING)
        fun provideEnabledLogging(): Boolean = false

        @Provides
        @Singleton
        fun provideLogger(@Named(ENABLE_LOGGING) enableLogging: Boolean) =
            Logger.getInstance(enableLogging)

        /**
         * [FlowController]'s clientSecret might be updated multiple times through
         * [FlowController.configureWithSetupIntent] or [FlowController.configureWithPaymentIntent].
         *
         * Should always be injected with [Provider].
         */
        @Provides
        fun provideClientSecret(
            viewModel: FlowControllerViewModel
        ): ClientSecret {
            return viewModel.initData.clientSecret
        }

        @Provides
        @Singleton
        fun providePrefsRepositoryFactory(
            appContext: Context,
            @IOContext workContext: CoroutineContext
        ): (PaymentSheet.CustomerConfiguration?) -> PrefsRepository = { customerConfig ->
            customerConfig?.let {
                DefaultPrefsRepository(
                    appContext,
                    it.id,
                    workContext
                )
            } ?: PrefsRepository.Noop()
        }

        @Provides
        @Singleton
        fun provideAnalyticsRequestFactory(
            appContext: Context,
            lazyPaymentConfiguration: Lazy<PaymentConfiguration>
        ) = AnalyticsRequestFactory(
            appContext,
            { lazyPaymentConfiguration.get().publishableKey },
            setOf(PaymentSheetEvent.PRODUCT_USAGE)
        )

        @Provides
        @IOContext
        fun provideWorkContext(): CoroutineContext = Dispatchers.IO

        @Provides
        @Singleton
        fun provideEventReporterMode(): EventReporter.Mode = EventReporter.Mode.Custom

        @Provides
        @Singleton
        fun provideViewModel(viewModelStoreOwner: ViewModelStoreOwner): FlowControllerViewModel =
            ViewModelProvider(viewModelStoreOwner)[FlowControllerViewModel::class.java]
    }
}
