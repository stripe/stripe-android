package com.stripe.android.paymentsheet.injection

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.PaymentConfiguration
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.payments.core.injection.ENABLE_LOGGING
import com.stripe.android.paymentsheet.DefaultGooglePayRepository
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.GooglePayRepository
import com.stripe.android.paymentsheet.analytics.DefaultDeviceIdRepository
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.SessionId
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
import javax.inject.Singleton

@Module
internal class FlowControllerModule {
    @Provides
    @Named(ENABLE_LOGGING)
    fun provideEnabledLogging(): Boolean = false

    @Provides
    @Singleton
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
            { customerId: String, isGooglePayReady: Boolean ->
                DefaultPrefsRepository(
                    appContext,
                    customerId,
                    { isGooglePayReady },
                    Dispatchers.IO
                )
            },
            isGooglePayReadySupplier =
            { environment ->
                val googlePayRepository = environment?.let {
                    DefaultGooglePayRepository(
                        appContext,
                        it
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
        sessionId: SessionId,
        appContext: Context,
        lazyPaymentConfiguration: Lazy<PaymentConfiguration>
    ): EventReporter {
        return DefaultEventReporter(
            mode = EventReporter.Mode.Custom,
            sessionId,
            DefaultDeviceIdRepository(appContext, Dispatchers.IO),
            AnalyticsRequestExecutor.Default(),
            AnalyticsRequestFactory(
                appContext
            ) { lazyPaymentConfiguration.get().publishableKey },
            Dispatchers.IO
        )
    }

    @Provides
    @Singleton
    fun provideSessionId() = SessionId()

    @Provides
    @Singleton
    fun provideViewModel(viewModelStoreOwner: ViewModelStoreOwner): FlowControllerViewModel =
        ViewModelProvider(viewModelStoreOwner)[FlowControllerViewModel::class.java]



}
