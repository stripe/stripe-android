package com.stripe.android.paymentsheet.injection

import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.IS_LIVE_MODE
import com.stripe.android.paymentsheet.analytics.EventReporter
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

@Module(
    subcomponents = [
        PaymentOptionsViewModelSubcomponent::class,
    ]
)
internal class PaymentOptionsViewModelModule {

    @Provides
    @Singleton
    fun provideEventReporterMode(): EventReporter.Mode = EventReporter.Mode.Custom

    @Provides
    @Named(IS_LIVE_MODE)
    fun isLiveMode(
        paymentConfiguration: Provider<PaymentConfiguration>
    ): () -> Boolean = { paymentConfiguration.get().isLiveMode() }
}
