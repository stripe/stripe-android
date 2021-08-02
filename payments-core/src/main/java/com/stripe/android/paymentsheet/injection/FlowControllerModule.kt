package com.stripe.android.paymentsheet.injection

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.PaymentConfiguration
import com.stripe.android.googlepaylauncher.DefaultGooglePayRepository
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.payments.core.injection.ENABLE_LOGGING
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.FlowController
import com.stripe.android.paymentsheet.analytics.DefaultDeviceIdRepository
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.flowcontroller.DefaultFlowControllerInitializer
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerInitializer
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerViewModel
import com.stripe.android.paymentsheet.model.ClientSecret
import dagger.Lazy
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

@Module
internal class FlowControllerModule {
    @Provides
    @Named(ENABLE_LOGGING)
    fun provideEnabledLogging(): Boolean = false

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
    fun provideFlowControllerInitializer(appContext: Context): FlowControllerInitializer {
        return DefaultFlowControllerInitializer(
            prefsRepositoryFactory =
            { customerId: String ->
                DefaultPrefsRepository(
                    appContext,
                    customerId,
                    Dispatchers.IO
                )
            },
            isGooglePayReadySupplier =
            { environment ->
                val googlePayRepository = environment?.let {
                    DefaultGooglePayRepository(
                        appContext,
                        when (environment) {
                            PaymentSheet.GooglePayConfiguration.Environment.Production ->
                                GooglePayEnvironment.Production
                            PaymentSheet.GooglePayConfiguration.Environment.Test ->
                                GooglePayEnvironment.Test
                        }
                    )
                } ?: GooglePayRepository.Disabled
                googlePayRepository.isReady().first()
            },
            workContext = Dispatchers.IO
        )
    }

    @Provides
    @Singleton
    fun provideEventReporter(
        appContext: Context,
        lazyPaymentConfiguration: Lazy<PaymentConfiguration>
    ): EventReporter {
        return DefaultEventReporter(
            mode = EventReporter.Mode.Custom,
            DefaultDeviceIdRepository(appContext, Dispatchers.IO),
            AnalyticsRequestExecutor.Default(),
            AnalyticsRequestFactory(
                appContext,
                { lazyPaymentConfiguration.get().publishableKey }
            ),
            Dispatchers.IO
        )
    }

    @Provides
    @Singleton
    fun provideViewModel(viewModelStoreOwner: ViewModelStoreOwner): FlowControllerViewModel =
        ViewModelProvider(viewModelStoreOwner)[FlowControllerViewModel::class.java]
}
