package com.stripe.android.paymentsheet.injection

import android.app.Application
import android.content.Context
import com.stripe.android.payments.core.injection.IOContext
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.ClientSecret
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module
internal abstract class PaymentSheetViewModelModule {

    @Binds
    abstract fun bindsApplicationForContext(application: Application): Context

    companion object {
        @Provides
        @Singleton
        fun provideClientSecret(
            starterArgs: PaymentSheetContract.Args
        ): ClientSecret {
            return starterArgs.clientSecret
        }

        @Provides
        @Singleton
        fun providePrefsRepository(
            appContext: Context,
            starterArgs: PaymentSheetContract.Args,
            @IOContext workContext: CoroutineContext
        ): PrefsRepository {
            return starterArgs.config?.customer?.let { (id) ->
                DefaultPrefsRepository(
                    appContext,
                    customerId = id,
                    workContext = workContext
                )
            } ?: PrefsRepository.Noop()
        }

        @Provides
        @Singleton
        fun provideEventReporterMode(): EventReporter.Mode = EventReporter.Mode.Complete

        @Provides
        @Singleton
        @Named(PRODUCT_USAGE)
        fun provideProductUsageTokens() = setOf("PaymentSheet")
    }
}
