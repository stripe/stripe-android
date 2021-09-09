package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.payments.core.injection.IOContext
import com.stripe.android.payments.core.injection.PUBLISHABLE_KEY
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.analytics.EventReporter
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module
internal class PaymentOptionsViewModelModule {
    @Provides
    fun providePaymentConfiguration(appContext: Context): PaymentConfiguration {
        return PaymentConfiguration.getInstance(appContext)
    }

    @Provides
    @Named(PUBLISHABLE_KEY)
    fun providePublishableKey(paymentConfiguration: PaymentConfiguration): () -> String {
        return { paymentConfiguration.publishableKey }
    }

    @Provides
    @Singleton
    fun provideEventReporterMode(): EventReporter.Mode = EventReporter.Mode.Custom

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
}
